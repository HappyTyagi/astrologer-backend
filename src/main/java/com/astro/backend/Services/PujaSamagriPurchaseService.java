package com.astro.backend.Services;

import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.PujaSamagriCart;
import com.astro.backend.Entity.PujaSamagriMaster;
import com.astro.backend.Entity.PujaSamagriPurchase;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.PujaSamagriCartRepository;
import com.astro.backend.Repositry.PujaSamagriMasterRepository;
import com.astro.backend.Repositry.PujaSamagriPurchaseRepository;
import com.astro.backend.RequestDTO.PujaSamagriPurchaseRequest;
import com.astro.backend.ResponseDTO.PujaSamagriPurchaseResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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
public class PujaSamagriPurchaseService {

    private final PujaSamagriMasterRepository masterRepository;
    private final PujaSamagriPurchaseRepository purchaseRepository;
    private final AddressRepository addressRepository;
    private final PujaSamagriCartRepository cartRepository;
    private final WalletService walletService;
    private final OrderHistoryService orderHistoryService;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final EmailService emailService;

    @Transactional
    public PujaSamagriPurchaseResponse purchaseCart(PujaSamagriPurchaseRequest request) {
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
        sendReceiptIfEmailAvailable(saved);

        return PujaSamagriPurchaseResponse.builder()
                .orderId(orderId)
                .userId(request.getUserId())
                .addressId(request.getAddressId())
                .totalItems(totalQuantity)
                .totalAmount(totalAmount)
                .payableAmount(totalAmount)
                .paymentMethod(finalPaymentMethod)
                .transactionId(finalTransactionId)
                .walletUsed(walletUsed)
                .gatewayPaid(gatewayPaid)
                .purchasedAt(purchaseTime)
                .purchases(saved)
                .build();
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

    private void sendReceiptIfEmailAvailable(List<PujaSamagriPurchase> purchases) {
        try {
            if (purchases == null || purchases.isEmpty()) {
                return;
            }
            final PujaSamagriPurchase first = purchases.get(0);
            if (first.getUserId() == null || first.getUserId() <= 0) {
                return;
            }

            final MobileUserProfile profile = mobileUserProfileRepository.findByUserId(first.getUserId()).orElse(null);
            final String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) {
                return;
            }

            final String orderCode = shortOrderId(first.getOrderId());
            final String subject = "Your Astrologer Puja Samagri Receipt - " + orderCode;
            final String html = buildReceiptHtml(purchases);
            final byte[] pdf = buildReceiptPdf(purchases);
            final String fileName = "puja-samagri-receipt-" + orderCode.replace("#", "") + ".pdf";

            emailService.sendEmailWithAttachmentAsync(
                    toEmail,
                    subject,
                    html,
                    fileName,
                    pdf,
                    "application/pdf"
            );
        } catch (Exception ignored) {
            // Purchase flow should not fail if receipt email cannot be queued.
        }
    }

    private String buildReceiptHtml(List<PujaSamagriPurchase> purchases) {
        final PujaSamagriPurchase first = purchases.get(0);
        final String currency = valueOrDefault(first.getCurrency(), "INR");
        final String orderCode = shortOrderId(first.getOrderId());
        final String orderDate = formatDateTime(first.getPurchasedAt());
        final String paymentMethod = valueOrDefault(first.getPaymentMethod(), "NA");
        final String transactionId = valueOrDefault(first.getTransactionId(), "NA");
        final String addressText = formatAddress(first.getAddress());

        double subtotal = 0.0;
        final StringBuilder rows = new StringBuilder();
        int sr = 1;
        for (PujaSamagriPurchase item : purchases) {
            final int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            final double unitPrice = item.getFinalUnitPrice() == null ? 0.0 : item.getFinalUnitPrice();
            final double line = item.getLineTotal() == null ? 0.0 : item.getLineTotal();
            subtotal += line;

            rows.append("""
                        <tr>
                          <td>%d</td>
                          <td>%s</td>
                          <td style="text-align:center;">%d</td>
                          <td style="text-align:right;">%s</td>
                          <td style="text-align:right;">%s</td>
                        </tr>
                    """.formatted(
                    sr++,
                    escapeHtml(valueOrDefault(item.getName(), "Puja Samagri")),
                    quantity,
                    escapeHtml(formatMoney(unitPrice, currency)),
                    escapeHtml(formatMoney(line, currency))
            ));
        }

        return """
                <html>
                <body style="margin:0;padding:0;background:#f3f6fc;font-family:Arial,sans-serif;color:#1f2a44;">
                  <div style="max-width:820px;margin:0 auto;padding:18px;">
                    <div style="background:#ffffff;border:1px solid #dbe4f6;border-radius:14px;padding:20px;">
                      <h2 style="margin:0;color:#1d3c8b;">Puja Samagri Purchase Receipt</h2>
                      <p style="margin:6px 0 0 0;color:#5b6785;">Thank you for shopping with Astro Adhyaay.</p>
                      <hr style="border:none;border-top:1px solid #e5ebf7;margin:14px 0 16px 0;"/>
                      <table style="width:100%%;font-size:14px;line-height:1.5;">
                        <tr><td><strong>Order</strong></td><td>%s</td></tr>
                        <tr><td><strong>Date</strong></td><td>%s</td></tr>
                        <tr><td><strong>Payment Method</strong></td><td>%s</td></tr>
                        <tr><td><strong>Transaction</strong></td><td>%s</td></tr>
                        <tr><td><strong>Delivery Address</strong></td><td>%s</td></tr>
                      </table>
                
                      <div style="margin-top:16px;border:1px solid #dbe4f6;border-radius:10px;overflow:hidden;">
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <thead>
                            <tr style="background:#1d3c8b;color:#ffffff;">
                              <th style="padding:10px;text-align:left;">#</th>
                              <th style="padding:10px;text-align:left;">Item</th>
                              <th style="padding:10px;text-align:center;">Qty</th>
                              <th style="padding:10px;text-align:right;">Unit Price</th>
                              <th style="padding:10px;text-align:right;">Amount</th>
                            </tr>
                          </thead>
                          <tbody>
                            %s
                          </tbody>
                        </table>
                      </div>
                
                      <div style="margin-top:16px;text-align:right;font-size:15px;">
                        <div><strong>Total:</strong> %s</div>
                      </div>
                
                      <p style="margin-top:18px;font-size:12px;color:#6d7691;">
                        PDF invoice is attached with this email. If you need help, reply to this mail.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(orderCode),
                escapeHtml(orderDate),
                escapeHtml(paymentMethod),
                escapeHtml(transactionId),
                escapeHtml(addressText),
                rows.toString(),
                escapeHtml(formatMoney(subtotal, currency))
        );
    }

    private byte[] buildReceiptPdf(List<PujaSamagriPurchase> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            return new byte[0];
        }

        final PujaSamagriPurchase first = purchases.get(0);
        final String currency = valueOrDefault(first.getCurrency(), "INR");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 30, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(29, 60, 139));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(63, 78, 112));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(29, 39, 64));

            Paragraph title = new Paragraph("Puja Samagri Purchase Receipt", titleFont);
            title.setSpacingAfter(10f);
            document.add(title);

            PdfPTable meta = new PdfPTable(new float[]{1f, 2.2f});
            meta.setWidthPercentage(100f);
            meta.setSpacingAfter(12f);
            meta.addCell(metaCell("Order", labelFont));
            meta.addCell(metaCell(shortOrderId(first.getOrderId()), valueFont));
            meta.addCell(metaCell("Date", labelFont));
            meta.addCell(metaCell(formatDateTime(first.getPurchasedAt()), valueFont));
            meta.addCell(metaCell("Payment Method", labelFont));
            meta.addCell(metaCell(valueOrDefault(first.getPaymentMethod(), "NA"), valueFont));
            meta.addCell(metaCell("Transaction", labelFont));
            meta.addCell(metaCell(valueOrDefault(first.getTransactionId(), "NA"), valueFont));
            meta.addCell(metaCell("Address", labelFont));
            meta.addCell(metaCell(formatAddress(first.getAddress()), valueFont));
            document.add(meta);

            PdfPTable table = new PdfPTable(new float[]{0.7f, 2.5f, 0.9f, 1.2f, 1.2f});
            table.setWidthPercentage(100f);
            table.setSpacingBefore(4f);
            table.setSpacingAfter(12f);

            table.addCell(headerCell("#"));
            table.addCell(headerCell("Item"));
            table.addCell(headerCell("Qty"));
            table.addCell(headerCell("Unit Price"));
            table.addCell(headerCell("Amount"));

            int sr = 1;
            double total = 0.0;
            for (PujaSamagriPurchase item : purchases) {
                final int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                final double unit = item.getFinalUnitPrice() == null ? 0.0 : item.getFinalUnitPrice();
                final double line = item.getLineTotal() == null ? 0.0 : item.getLineTotal();
                total += line;

                table.addCell(bodyCell(String.valueOf(sr++), Element.ALIGN_CENTER));
                table.addCell(bodyCell(valueOrDefault(item.getName(), "Puja Samagri"), Element.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(qty), Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatMoney(unit, currency), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(line, currency), Element.ALIGN_RIGHT));
            }
            document.add(table);

            Paragraph totalPara = new Paragraph("Total: " + formatMoney(total, currency), titleFont);
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalPara);

            Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(104, 112, 130));
            Paragraph foot = new Paragraph(
                    "This is a system generated receipt. For support, contact Astro Adhyaay support.",
                    footFont
            );
            foot.setSpacingBefore(10f);
            document.add(foot);

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    private PdfPCell metaCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDefault(value, "-"), font));
        cell.setPadding(6f);
        cell.setBorderColor(new Color(223, 231, 248));
        return cell;
    }

    private PdfPCell headerCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7f);
        cell.setBackgroundColor(new Color(29, 60, 139));
        cell.setBorderColor(new Color(29, 60, 139));
        return cell;
    }

    private PdfPCell bodyCell(String text, int align) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(29, 39, 64));
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDefault(text, "-"), font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBorderColor(new Color(223, 231, 248));
        return cell;
    }

    private String shortOrderId(String orderId) {
        String raw = orderId == null ? "" : orderId.trim();
        if (raw.isEmpty()) {
            return "#NA";
        }
        String compact = raw.replace("-", "");
        if (compact.length() > 8) {
            compact = compact.substring(0, 8).toUpperCase(Locale.ROOT);
        } else {
            compact = compact.toUpperCase(Locale.ROOT);
        }
        return "#" + compact;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return dateTime.format(formatter);
    }

    private String formatMoney(double amount, String currency) {
        String cur = valueOrDefault(currency, "INR");
        return cur + " " + String.format(Locale.ENGLISH, "%.2f", amount);
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        if (address.getAddressLine1() != null && !address.getAddressLine1().isBlank()) {
            parts.add(address.getAddressLine1().trim());
        }
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) {
            parts.add(address.getAddressLine2().trim());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            parts.add(address.getCity().trim());
        }
        if (address.getState() != null && !address.getState().isBlank()) {
            parts.add(address.getState().trim());
        }
        if (address.getPincode() != null && !address.getPincode().isBlank()) {
            parts.add(address.getPincode().trim());
        }
        if (parts.isEmpty()) {
            return "-";
        }
        return String.join(", ", parts);
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String escapeHtml(String value) {
        String text = valueOrDefault(value, "");
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
