package com.astro.backend.Services;

import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.Remides;
import com.astro.backend.Entity.RemidesCart;
import com.astro.backend.Entity.RemidesPurchase;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.RemidesCartRepository;
import com.astro.backend.Repositry.RemidesPurchaseRepository;
import com.astro.backend.Repositry.RemidesRepository;
import com.astro.backend.RequestDTO.RemidesPurchaseRequest;
import com.astro.backend.RequestDTO.ResendReceiptRequest;
import com.astro.backend.ResponseDTO.RemidesPurchaseHistoryResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RemidesPurchaseService {

    private final RemidesRepository remidesRepository;
    private final RemidesPurchaseRepository remidesPurchaseRepository;
    private final AddressRepository addressRepository;
    private final RemidesCartRepository remidesCartRepository;
    private final EmailService emailService;
    private final OrderHistoryService orderHistoryService;
    private final WalletService walletService;

    @Transactional
    public Map<String, Object> purchaseCart(RemidesPurchaseRequest request) {
        validateRequest(request);

        final String orderId = UUID.randomUUID().toString();
        final LocalDateTime purchaseTime = LocalDateTime.now();
        final Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found: " + request.getAddressId()));

        final List<RemidesPurchase> purchases = new ArrayList<>();
        double totalAmount = 0.0;
        int totalQuantity = 0;

        for (RemidesPurchaseRequest.PurchaseItem item : request.getItems()) {
            Remides remides = remidesRepository.findById(item.getRemidesId())
                    .orElseThrow(() -> new RuntimeException("Remides not found: " + item.getRemidesId()));

            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            double unitPrice = remides.getPrice() == null ? 0.0 : remides.getPrice();
            double finalUnitPrice = remides.getFinalPrice() == null ? unitPrice : remides.getFinalPrice();
            double lineTotal = finalUnitPrice * quantity;

            totalAmount += lineTotal;
            totalQuantity += quantity;

            purchases.add(RemidesPurchase.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .remides(remides)
                    .address(address)
                    .title(remides.getTitle())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountPercentage(remides.getDiscountPercentage())
                    .finalUnitPrice(finalUnitPrice)
                    .lineTotal(lineTotal)
                    .currency(remides.getCurrency())
                    .status("COMPLETED")
                    .purchasedAt(purchaseTime)
                    .build());
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

        if (isWalletPayment) {
            boolean debited = walletService.debit(
                    request.getUserId(),
                    totalAmount,
                    "REMEDY_PURCHASE",
                    "Remedies order payment"
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
                            "REMEDY_PURCHASE",
                            "Remedies order payment (wallet part)"
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

        for (RemidesPurchase p : purchases) {
            p.setPaymentMethod(finalPaymentMethod);
            p.setTransactionId(finalTransactionId);
            p.setWalletUsed(walletUsed);
            p.setGatewayPaid(gatewayPaid);
        }

        List<RemidesPurchase> saved = remidesPurchaseRepository.saveAll(purchases);
        orderHistoryService.recordRemedyPurchases(saved);
        deactivateUserCart(request.getUserId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Products purchased successfully");
        response.put("orderId", orderId);
        response.put("userId", request.getUserId());
        response.put("addressId", request.getAddressId());
        response.put("totalItems", totalQuantity);
        response.put("totalAmount", totalAmount);
        response.put("paymentMethod", finalPaymentMethod);
        response.put("transactionId", finalTransactionId);
        response.put("walletUsed", walletUsed);
        response.put("gatewayPaid", gatewayPaid);
        response.put("purchasedAt", purchaseTime);
        response.put("purchases", saved);
        return response;
    }

    private void deactivateUserCart(Long userId) {
        List<RemidesCart> activeCartItems =
                remidesCartRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        if (activeCartItems.isEmpty()) {
            return;
        }
        for (RemidesCart cartItem : activeCartItems) {
            cartItem.setIsActive(false);
        }
        remidesCartRepository.saveAll(activeCartItems);
    }

    @Transactional
    public List<RemidesPurchaseHistoryResponse> getPurchaseHistory(Long userId) {
        return orderHistoryService.getUserHistory(userId);
    }

    @Transactional
    public Map<String, Object> getPurchaseHistoryRealtime(Long userId, LocalDateTime since) {
        List<RemidesPurchaseHistoryResponse> data = since == null
                ? orderHistoryService.getUserHistory(userId)
                : orderHistoryService.getUserHistorySince(userId, since);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("serverTime", LocalDateTime.now());
        response.put("count", data.size());
        response.put("items", data);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resendReceiptEmail(String orderId, ResendReceiptRequest request) {
        if (orderId == null || orderId.isBlank()) {
            throw new RuntimeException("Valid orderId is required");
        }
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        final String toEmail = request.getEmail() == null ? "" : request.getEmail().trim();
        if (toEmail.isEmpty()) {
            throw new RuntimeException("Valid email is required");
        }

        final String resolvedOrderId = resolveOrderId(orderId);
        final List<RemidesPurchase> purchases = remidesPurchaseRepository.findByOrderIdOrderByIdAsc(resolvedOrderId);
        if (purchases.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        final Long orderUserId = purchases.get(0).getUserId();
        if (!Objects.equals(orderUserId, request.getUserId())) {
            throw new RuntimeException("Order does not belong to this user");
        }

        final String subject = "Your Astrologer Remedy Receipt - " + shortOrderId(resolvedOrderId);
        final String html = buildPremiumReceiptHtml(purchases);
        final byte[] pdf = buildReceiptPdf(purchases);
        final String fileName = "receipt-" + shortOrderId(resolvedOrderId).replace("#", "") + ".pdf";

        emailService.sendEmailWithAttachmentAsync(
                toEmail,
                subject,
                html,
                fileName,
                pdf,
                "application/pdf"
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", true);
        response.put("message", "Receipt mail request sent successfully. You will receive the email shortly.");
        response.put("queued", true);
        response.put("orderId", resolvedOrderId);
        response.put("requestedOrderCode", orderId);
        response.put("email", toEmail);
        response.put("requestedAt", LocalDateTime.now());
        return response;
    }

    private String resolveOrderId(String rawOrderId) {
        final String input = rawOrderId == null ? "" : rawOrderId.trim();
        if (input.isEmpty()) {
            throw new RuntimeException("Valid orderId is required");
        }

        // Direct DB orderId (UUID) path
        if (remidesPurchaseRepository.findFirstByOrderIdOrderByIdDesc(input).isPresent()) {
            return input;
        }

        // Accept display code like #OC90EEAF
        final String upper = input.toUpperCase(Locale.ROOT);
        final String compactPrefix;
        if (upper.startsWith("#OC")) {
            compactPrefix = upper.substring(3).replaceAll("[^A-Z0-9]", "");
        } else if (!input.contains("-")) {
            compactPrefix = upper.replaceAll("[^A-Z0-9]", "");
        } else {
            compactPrefix = "";
        }

        if (compactPrefix.length() >= 4) {
            final String matched = remidesPurchaseRepository.findLatestOrderIdByCompactPrefix(compactPrefix);
            if (matched != null && !matched.isBlank()) {
                return matched;
            }
        }

        throw new RuntimeException("Order not found: " + rawOrderId);
    }

    private void validateRequest(RemidesPurchaseRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body is required");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new RuntimeException("Valid userId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }
        if (request.getAddressId() == null || request.getAddressId() <= 0) {
            throw new RuntimeException("Valid addressId is required");
        }
    }

    private String buildPremiumReceiptHtml(List<RemidesPurchase> purchases) {
        final RemidesPurchase first = purchases.get(0);
        final String currency = first.getCurrency() == null ? "INR" : first.getCurrency();
        final String orderCode = shortOrderId(first.getOrderId());
        final String date = first.getPurchasedAt() == null
                ? "-"
                : first.getPurchasedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

        double originalTotal = 0.0;
        double finalTotal = 0.0;
        int totalQty = 0;
        StringBuilder rows = new StringBuilder();

        for (RemidesPurchase item : purchases) {
            int qty = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            double unit = item.getUnitPrice() == null ? 0.0 : item.getUnitPrice();
            double fUnit = item.getFinalUnitPrice() == null ? unit : item.getFinalUnitPrice();
            double line = item.getLineTotal() == null ? (fUnit * qty) : item.getLineTotal();
            originalTotal += unit * qty;
            finalTotal += line;
            totalQty += qty;

            rows.append("<tr>")
                    .append("<td>").append(escape(item.getTitle())).append("</td>")
                    .append("<td style=\"text-align:center;\">").append(qty).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatMoney(unit, currency)).append("</td>")
                    .append("<td style=\"text-align:right; font-weight:700;\">").append(formatMoney(line, currency)).append("</td>")
                    .append("</tr>");
        }

        double discount = Math.max(0.0, originalTotal - finalTotal);
        String addressBlock = "-";
        if (first.getAddress() != null) {
            Address a = first.getAddress();
            List<String> parts = new ArrayList<>();
            if (a.getAddressLine1() != null && !a.getAddressLine1().isBlank()) parts.add(a.getAddressLine1());
            if (a.getAddressLine2() != null && !a.getAddressLine2().isBlank()) parts.add(a.getAddressLine2());
            if (a.getDistrict() != null && !a.getDistrict().isBlank()) parts.add(a.getDistrict());
            if (a.getCity() != null && !a.getCity().isBlank()) parts.add(a.getCity());
            if (a.getState() != null && !a.getState().isBlank()) parts.add(a.getState());
            if (a.getPincode() != null && !a.getPincode().isBlank()) parts.add(a.getPincode());
            addressBlock = escape(String.join(", ", parts));
        }

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <style>
                    body { margin:0; background:#f4f2eb; font-family:Calibri, Arial, sans-serif; color:#1f1f1f; }
                    .wrap { max-width:760px; margin:28px auto; padding:0 16px; }
                    .card { background:#ffffff; border-radius:20px; border:1px solid #ece7d8; overflow:hidden; box-shadow:0 8px 30px rgba(35,29,14,.08);}
                    .head { padding:22px; background:linear-gradient(135deg,#1f1f1f,#3a3528); color:#fff; }
                    .brand { font-size:22px; font-weight:700; letter-spacing:.3px; }
                    .sub { margin-top:6px; opacity:.9; font-size:14px; }
                    .body { padding:22px; }
                    .meta { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:16px; font-size:14px; }
                    .pill { display:inline-block; background:#f6f4ed; border:1px solid #e8dfc9; border-radius:999px; padding:6px 12px; font-weight:700; }
                    table { width:100%; border-collapse:collapse; margin-top:14px; }
                    th,td { padding:10px 8px; border-bottom:1px solid #efe9dc; font-size:14px; }
                    th { text-align:left; color:#6f6a5d; font-weight:700; background:#faf8f2; }
                    .sum { margin-top:14px; margin-left:auto; max-width:320px; }
                    .row { display:flex; justify-content:space-between; padding:7px 0; font-size:14px; }
                    .total { border-top:1px solid #e8dfc9; margin-top:6px; padding-top:10px; font-size:19px; font-weight:700; }
                    .addr { margin-top:16px; padding:12px; background:#faf8f2; border-radius:12px; border:1px solid #efe9dc; font-size:13px; color:#474235; }
                    .foot { margin-top:16px; font-size:12px; color:#847e70; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="head">
                        <div class="brand">Astrologer - Remedy Receipt</div>
                        <div class="sub">Thank you for your purchase. Your PDF invoice is attached.</div>
                      </div>
                      <div class="body">
                        <div class="meta">
                          <div><strong>Order:</strong> %s</div>
                          <div><strong>Date:</strong> %s</div>
                          <div><strong>Status:</strong> <span class="pill">%s</span></div>
                          <div><strong>Total Items:</strong> %d</div>
                        </div>
                        <table>
                          <thead>
                            <tr><th>Item</th><th style="text-align:center;">Qty</th><th style="text-align:right;">Unit</th><th style="text-align:right;">Amount</th></tr>
                          </thead>
                          <tbody>%s</tbody>
                        </table>
                        <div class="sum">
                          <div class="row"><span>Subtotal</span><span>%s</span></div>
                          <div class="row"><span>Discount</span><span style="color:#1f9d63;">-%s</span></div>
                          <div class="row total"><span>Total</span><span>%s</span></div>
                        </div>
                        <div class="addr"><strong>Delivery Address:</strong><br/>%s</div>
                        <div class="foot">Need help? Reply to this email and our team will assist you.</div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escape(orderCode),
                escape(date),
                escape(purchases.get(0).getStatus()),
                totalQty,
                rows.toString(),
                escape(formatMoney(originalTotal, currency)),
                escape(formatMoney(discount, currency)),
                escape(formatMoney(finalTotal, currency)),
                addressBlock
        );
    }

    private byte[] buildReceiptPdf(List<RemidesPurchase> purchases) {
        final RemidesPurchase first = purchases.get(0);
        final String currency = first.getCurrency() == null ? "INR" : first.getCurrency();
        double originalTotal = 0.0;
        double finalTotal = 0.0;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            Font h1 = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(37, 32, 24));
            Font body = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(45, 45, 45));
            Font bold = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(35, 35, 35));

            document.add(new Paragraph("Astrologer - Remedy Receipt", h1));
            document.add(new Paragraph("Order: " + shortOrderId(first.getOrderId()), body));
            document.add(new Paragraph("Date: " + (
                    first.getPurchasedAt() == null
                            ? "-"
                            : first.getPurchasedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
            ), body));
            document.add(new Paragraph("Status: " + first.getStatus(), body));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{4f, 1f, 2f, 2f});
            table.setWidthPercentage(100);
            table.addCell(headerCell("Item"));
            table.addCell(headerCell("Qty"));
            table.addCell(headerCell("Unit"));
            table.addCell(headerCell("Amount"));

            for (RemidesPurchase item : purchases) {
                int qty = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
                double unit = item.getUnitPrice() == null ? 0.0 : item.getUnitPrice();
                double fUnit = item.getFinalUnitPrice() == null ? unit : item.getFinalUnitPrice();
                double line = item.getLineTotal() == null ? (fUnit * qty) : item.getLineTotal();
                originalTotal += unit * qty;
                finalTotal += line;

                table.addCell(bodyCell(item.getTitle(), Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(qty), Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatMoney(unit, currency), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(line, currency), Element.ALIGN_RIGHT));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);
            double discount = Math.max(0.0, originalTotal - finalTotal);
            document.add(new Paragraph("Subtotal: " + formatMoney(originalTotal, currency), body));
            document.add(new Paragraph("Discount: -" + formatMoney(discount, currency), body));
            document.add(new Paragraph("Total: " + formatMoney(finalTotal, currency), bold));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell headerCell(String value) {
        Font font = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(35, 35, 35));
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell bodyCell(String value, int align) {
        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(45, 45, 45));
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(7f);
        return cell;
    }

    private String shortOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) return "#ORDER";
        String compact = orderId.replace("-", "").toUpperCase();
        String suffix = compact.length() >= 6 ? compact.substring(0, 6) : compact;
        return "#OC" + suffix;
    }

    private String formatMoney(double amount, String currency) {
        String c = currency == null ? "INR" : currency.toUpperCase();
        String symbol = "INR".equals(c) ? "₹" : c + " ";
        return symbol + String.format(Locale.US, "%.2f", amount);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
