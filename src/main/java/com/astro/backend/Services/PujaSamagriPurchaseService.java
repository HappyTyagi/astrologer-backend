package com.astro.backend.Services;

import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.PujaSamagriCart;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Entity.PujaSamagriPurchase;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.PujaSamagriCartRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.Repositry.PujaSamagriPurchaseRepository;
import com.astro.backend.RequestDTO.PujaSamagriPurchaseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PujaSamagriPurchaseService {

    private final PujaSamagriMasterRepository masterRepository;
    private final PujaSamagriPurchaseRepository purchaseRepository;
    private final AddressRepository addressRepository;
    private final PujaSamagriCartRepository cartRepository;
    private final WalletService walletService;
    private final OrderHistoryService orderHistoryService;

    @Transactional
    public Map<String, Object> purchaseCart(PujaSamagriPurchaseRequest request) {
        validateRequest(request);

        final String orderId = UUID.randomUUID().toString();
        final LocalDateTime purchaseTime = LocalDateTime.now();
        final Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found: " + request.getAddressId()));

        final List<PujaSamagriPurchase> purchases = new ArrayList<>();
        double totalAmount = 0.0;
        int totalQuantity = 0;

        for (PujaSamagriPurchaseRequest.PurchaseItem item : request.getItems()) {
            if (item == null || item.getSamagriMasterId() == null) {
                continue;
            }
            PujaSamagriMaster master = masterRepository.findById(item.getSamagriMasterId())
                    .orElseThrow(() -> new RuntimeException("Samagri item not found: " + item.getSamagriMasterId()));

            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            double unitPrice = master.getPrice() == null ? 0.0 : master.getPrice();
            double finalUnitPrice = master.getFinalPrice() == null ? unitPrice : master.getFinalPrice();
            double lineTotal = finalUnitPrice * quantity;

            totalAmount += lineTotal;
            totalQuantity += quantity;

            purchases.add(PujaSamagriPurchase.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .samagriMaster(master)
                    .address(address)
                    .name(master.getName() == null ? "" : master.getName())
                    .hiName(master.getHiName())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountPercentage(master.getDiscountPercentage())
                    .finalUnitPrice(finalUnitPrice)
                    .lineTotal(lineTotal)
                    .currency(master.getCurrency() == null ? "INR" : master.getCurrency())
                    .status("COMPLETED")
                    .purchasedAt(purchaseTime)
                    .build());
        }

        if (purchases.isEmpty()) {
            throw new RuntimeException("No valid items in purchase request");
        }

        final String normalizedMethod = request.getPaymentMethod() == null
                ? "WALLET"
                : request.getPaymentMethod().trim().toUpperCase();
        final boolean wantsWallet = Boolean.TRUE.equals(request.getUseWallet());
        final boolean isWalletPayment = "WALLET".equals(normalizedMethod);
        final boolean isGatewayPayment = "GATEWAY".equals(normalizedMethod)
                || "UPI".equals(normalizedMethod)
                || "CARD".equals(normalizedMethod)
                || "NETBANKING".equals(normalizedMethod);

        if (!isWalletPayment && !isGatewayPayment) {
            throw new RuntimeException("Invalid paymentMethod. Use WALLET or GATEWAY.");
        }

        String finalTransactionId = request.getTransactionId() == null ? "" : request.getTransactionId().trim();
        String finalPaymentMethod = normalizedMethod;
        double walletUsed = 0.0;
        double gatewayPaid = 0.0;

        if (totalAmount < 0) {
            totalAmount = 0.0;
        }

        if (isWalletPayment) {
            boolean debited = walletService.debit(
                    request.getUserId(),
                    totalAmount,
                    "PUJA_SAMAGRI_PURCHASE",
                    "Puja samagri purchase"
            );
            if (!debited) {
                throw new RuntimeException("Insufficient wallet balance. Please add money to wallet or continue with gateway payment.");
            }
            walletUsed = totalAmount;
            if (finalTransactionId.isEmpty()) {
                finalTransactionId = "WALLET-" + UUID.randomUUID();
            }
            finalPaymentMethod = "WALLET";
        } else {
            if (wantsWallet) {
                Wallet wallet = walletService.getWallet(request.getUserId());
                double balance = wallet.getBalance();
                if (balance > 0) {
                    walletUsed = walletService.debitUpTo(
                            request.getUserId(),
                            totalAmount,
                            "PUJA_SAMAGRI_PURCHASE",
                            "Puja samagri purchase (wallet part)"
                    );
                }
            }

            gatewayPaid = Math.max(0.0, totalAmount - walletUsed);
            if (gatewayPaid > 0 && finalTransactionId.isEmpty()) {
                throw new RuntimeException("Gateway transactionId is required for remaining amount.");
            }
            if (gatewayPaid <= 0) {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "WALLET-" + UUID.randomUUID();
                }
                finalPaymentMethod = "WALLET";
            } else {
                if (finalTransactionId.isEmpty()) {
                    finalTransactionId = "GW-" + UUID.randomUUID();
                }
                finalPaymentMethod = walletUsed > 0 ? "WALLET+GATEWAY" : normalizedMethod;
            }
        }

        for (PujaSamagriPurchase p : purchases) {
            p.setPaymentMethod(finalPaymentMethod);
            p.setTransactionId(finalTransactionId);
            p.setWalletUsed(walletUsed);
            p.setGatewayPaid(gatewayPaid);
        }

        List<PujaSamagriPurchase> saved = purchaseRepository.saveAll(purchases);
        orderHistoryService.recordSamagriPurchases(saved);
        deactivateUserCart(request.getUserId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Products purchased successfully");
        response.put("orderId", orderId);
        response.put("userId", request.getUserId());
        response.put("addressId", request.getAddressId());
        response.put("totalItems", totalQuantity);
        response.put("totalAmount", totalAmount);
        response.put("payableAmount", totalAmount);
        response.put("paymentMethod", finalPaymentMethod);
        response.put("transactionId", finalTransactionId);
        response.put("walletUsed", walletUsed);
        response.put("gatewayPaid", gatewayPaid);
        response.put("purchasedAt", purchaseTime);
        response.put("purchases", saved);
        return response;
    }

    private void deactivateUserCart(Long userId) {
        List<PujaSamagriCart> activeCartItems =
                cartRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        if (activeCartItems.isEmpty()) {
            return;
        }
        for (PujaSamagriCart cartItem : activeCartItems) {
            cartItem.setIsActive(false);
        }
        cartRepository.saveAll(activeCartItems);
    }

    private void validateRequest(PujaSamagriPurchaseRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (request.getAddressId() == null || request.getAddressId() <= 0) {
            throw new RuntimeException("Valid addressId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("items are required");
        }
    }

    @Transactional(readOnly = true)
    public List<PujaSamagriPurchase> getPurchasesByOrderId(String orderId) {
        return purchaseRepository.findByOrderIdOrderByIdAsc(orderId);
    }
}
