package com.astro.backend.Services;

import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.Remides;
import com.astro.backend.Entity.RemidesCart;
import com.astro.backend.Entity.RemidesPurchase;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
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
import com.lowagie.text.FontFactory;
import com.lowagie.text.Anchor;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private final MobileUserProfileRepository mobileUserProfileRepository;
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
        sendReceiptIfEmailAvailable(saved);

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

    private void sendReceiptIfEmailAvailable(List<RemidesPurchase> purchases) {
        try {
            if (purchases == null || purchases.isEmpty()) {
                return;
            }
            RemidesPurchase first = purchases.get(0);
            if (first.getUserId() == null || first.getUserId() <= 0) {
                return;
            }

            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(first.getUserId()).orElse(null);
            String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) {
                return;
            }

            String subject = "Your Astrologer Remedy Receipt - " + shortOrderId(first.getOrderId());
            String html = buildPremiumReceiptHtml(purchases);
            byte[] pdf = buildReceiptPdf(purchases);
            String fileName = "receipt-" + shortOrderId(first.getOrderId()).replace("#", "") + ".pdf";

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
        final String toEmail = getVerifiedProfileEmail(request.getUserId(), request.getEmail());

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

    private String getVerifiedProfileEmail(Long userId, String requestedEmail) {
        MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId).orElse(null);
        String profileEmail = profile == null || profile.getEmail() == null
                ? ""
                : profile.getEmail().trim();
        if (profileEmail.isEmpty()) {
            throw new RuntimeException("No email is linked with this account. Please update profile email first.");
        }

        String provided = requestedEmail == null ? "" : requestedEmail.trim();
        if (!provided.isEmpty() && !profileEmail.equalsIgnoreCase(provided)) {
            throw new RuntimeException("Email verification failed. Please use your registered profile email.");
        }
        return profileEmail;
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
                    table { width:100%%; border-collapse:collapse; margin-top:14px; }
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
            Document document = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            addLogoWatermark(writer, document);

            FontFactory.registerDirectories();
            Font heroTitle = invoiceFont(24, Font.BOLD, Color.WHITE);
            Font heroSub = invoiceFont(11, Font.NORMAL, new Color(220, 231, 255));
            Font labelFont = invoiceFont(10, Font.BOLD, new Color(64, 76, 98));
            Font valueFont = invoiceFont(11, Font.NORMAL, new Color(22, 30, 48));
            Font sectionTitle = invoiceFont(11, Font.BOLD, new Color(20, 36, 90));
            Font tableHead = invoiceFont(10, Font.BOLD, Color.WHITE);
            Font tableBody = invoiceFont(10.5f, Font.NORMAL, new Color(33, 41, 62));
            Font amountBold = invoiceFont(12.5f, Font.BOLD, new Color(20, 36, 90));

            Color heroBg = new Color(24, 42, 96);
            Color panelBg = new Color(246, 249, 255);
            Color panelBorder = new Color(222, 231, 246);
            Color tableHeaderBg = new Color(45, 66, 137);

            PdfPTable hero = new PdfPTable(1);
            hero.setWidthPercentage(100);
            PdfPCell heroCell = new PdfPCell();
            heroCell.setBackgroundColor(heroBg);
            heroCell.setBorderColor(heroBg);
            heroCell.setPadding(16f);
            Paragraph heroTitleP = new Paragraph("Tax Invoice", heroTitle);
            heroTitleP.setSpacingAfter(5f);
            heroCell.addElement(heroTitleP);
            heroCell.addElement(new Paragraph("Remedies Purchase Receipt", invoiceFont(14, Font.BOLD, Color.WHITE)));
            heroCell.addElement(new Paragraph("ORIGINAL FOR RECIPIENT", heroSub));
            hero.addCell(heroCell);
            hero.setSpacingAfter(10f);
            document.add(hero);

            String orderRef = shortOrderId(first.getOrderId());
            String orderDate = first.getPurchasedAt() == null ? "-" : first.getPurchasedAt().toLocalDate().toString();

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(10f);
            meta.addCell(invoicePanelCell("Invoice No", orderRef, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Invoice Date", orderDate, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Payment Mode", valueOrDash(first.getPaymentMethod()), labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Status", valueOrDash(first.getStatus()), labelFont, valueFont, panelBg, panelBorder));
            document.add(meta);

            PdfPTable details = new PdfPTable(new float[]{1f, 1f});
            details.setWidthPercentage(100);
            details.setSpacingAfter(10f);
            details.addCell(invoiceDetailBlock(
                    "Billed To",
                    new String[]{
                            "Name: " + valueOrDash(first.getAddress() == null ? null : first.getAddress().getName()),
                            "Mobile: " + valueOrDash(first.getAddress() == null ? null : first.getAddress().getUserMobileNumber()),
                            "Address: " + buildAddressText(first.getAddress())
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            details.addCell(invoiceDetailBlock(
                    "Company Details",
                    new String[]{
                            "ASTROLOGER SERVICES PRIVATE LIMITED",
                            "GSTIN: 09ASTRO1234X1Z9",
                            "Email: support@astrologer.app",
                            "Service: Remedies Purchase",
                            "Region: India"
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            document.add(details);

            PdfPTable table = new PdfPTable(new float[]{1.1f, 5.8f, 2.1f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(10f);
            table.addCell(invoiceTableHeaderCell("Sr. No", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Particulars", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Amount", tableHead, tableHeaderBg));

            int sr = 1;
            for (RemidesPurchase item : purchases) {
                int qty = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
                double unit = item.getUnitPrice() == null ? 0.0 : item.getUnitPrice();
                double fUnit = item.getFinalUnitPrice() == null ? unit : item.getFinalUnitPrice();
                double line = item.getLineTotal() == null ? (fUnit * qty) : item.getLineTotal();
                originalTotal += unit * qty;
                finalTotal += line;

                table.addCell(invoiceTableTextCell(String.valueOf(sr++), tableBody, Element.ALIGN_CENTER, Color.WHITE, panelBorder));
                table.addCell(invoiceTableTextCell(
                        valueOrDash(item.getTitle()) + " (Qty: " + qty + ", Unit: " + formatMoney(unit, currency) + ")",
                        tableBody,
                        Element.ALIGN_LEFT,
                        Color.WHITE,
                        panelBorder
                ));
                table.addCell(invoiceTableTextCell(formatMoney(line, currency), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            }

            double discount = Math.max(0.0, originalTotal - finalTotal);

            table.addCell(invoiceTableTextCell("", tableBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell("Taxable Amount", tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell(formatMoney(originalTotal, currency), tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell("", tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            table.addCell(invoiceTableTextCell("Discount", tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            table.addCell(invoiceTableTextCell("-" + formatMoney(discount, currency), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            table.addCell(invoiceTableTextCell("", tableBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell("IGST @ 0.00%", tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell(formatMoney(0.0, currency), tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            table.addCell(invoiceTableTextCell("", tableBody, Element.ALIGN_LEFT, new Color(232, 239, 255), panelBorder));
            table.addCell(invoiceTableTextCell("Total Amount", amountBold, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));
            table.addCell(invoiceTableTextCell(formatMoney(finalTotal, currency), amountBold, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));

            document.add(table);

            Paragraph orderLinkPara = new Paragraph();
            orderLinkPara.setSpacingAfter(4f);
            orderLinkPara.add(new Chunk("Order Link: ", invoiceFont(10.5f, Font.BOLD, new Color(25, 47, 122))));
            String orderLink = "https://astrologer.app/orders/" + orderRef.replace("#", "");
            Anchor orderAnchor = new Anchor(orderLink, invoiceFont(10.5f, Font.UNDERLINE, new Color(25, 47, 122)));
            orderAnchor.setReference(orderLink);
            orderLinkPara.add(orderAnchor);
            document.add(orderLinkPara);

            Paragraph appPara = new Paragraph();
            appPara.setSpacingAfter(10f);
            appPara.add(new Chunk("App Link: ", invoiceFont(10.5f, Font.BOLD, new Color(25, 47, 122))));
            Anchor appAnchor = new Anchor("https://astrologer.app", invoiceFont(10.5f, Font.UNDERLINE, new Color(25, 47, 122)));
            appAnchor.setReference("https://astrologer.app");
            appPara.add(appAnchor);
            document.add(appPara);

            document.add(new Paragraph("This is a computer generated invoice and does not require signature.", invoiceFont(9.5f, Font.NORMAL, new Color(84, 93, 112))));
            document.add(new Paragraph("Need help? support@astrologer.app", invoiceFont(9.5f, Font.NORMAL, new Color(84, 93, 112))));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell invoiceHeaderCell(String value) {
        Font font = invoiceFont(11, Font.BOLD, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(238, 238, 238));
        cell.setPadding(8f);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(1.2f);
        return cell;
    }

    private PdfPCell invoiceBodyCell(String value, int align, Font font, float borderWidth) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(7f);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(borderWidth);
        return cell;
    }

    private Font invoiceFont(float size, int style, Color color) {
        Font font = FontFactory.getFont("Calibri", size, style, color);
        if (font == null || font.getFamilyname() == null || "unknown".equalsIgnoreCase(font.getFamilyname())) {
            return new Font(Font.HELVETICA, size, style, color);
        }
        return font;
    }

    private PdfPCell invoicePanelCell(
            String label,
            String value,
            Font labelFont,
            Font valueFont,
            Color background,
            Color border
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        Paragraph labelP = new Paragraph(valueOrDash(label), labelFont);
        labelP.setSpacingAfter(3f);
        cell.addElement(labelP);
        cell.addElement(new Paragraph(valueOrDash(value), valueFont));
        return cell;
    }

    private PdfPCell invoiceDetailBlock(
            String title,
            String[] lines,
            Font titleFont,
            Font lineFont,
            Color background,
            Color border
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        Paragraph titleP = new Paragraph(valueOrDash(title), titleFont);
        titleP.setSpacingAfter(6f);
        cell.addElement(titleP);
        for (String line : lines) {
            Paragraph row = new Paragraph(valueOrDash(line), lineFont);
            row.setSpacingAfter(2f);
            cell.addElement(row);
        }
        return cell;
    }

    private PdfPCell invoiceTableHeaderCell(String value, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDash(value), font));
        cell.setPadding(8f);
        cell.setBorderColor(background);
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment("Amount".equalsIgnoreCase(value) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell invoiceTableTextCell(String value, Font font, int align, Color background, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDash(value), font));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(border);
        cell.setBackgroundColor(background);
        return cell;
    }

    private void addLogoWatermark(PdfWriter writer, Document document) {
        try {
            byte[] logoBytes = loadLogoBytes();
            if (logoBytes == null || logoBytes.length == 0) {
                return;
            }
            Image watermark = Image.getInstance(logoBytes);
            watermark.scaleToFit(230, 230);

            Rectangle page = document.getPageSize();
            float x = (page.getWidth() - watermark.getScaledWidth()) / 2f;
            float y = (page.getHeight() - watermark.getScaledHeight()) / 2f;
            watermark.setAbsolutePosition(x, y);

            PdfContentByte under = writer.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.09f);
            under.saveState();
            under.setGState(state);
            under.addImage(watermark);
            under.restoreState();
        } catch (Exception ignored) {
            // Watermark is cosmetic; invoice generation should not fail because of this.
        }
    }

    private byte[] loadLogoBytes() {
        String[] candidates = new String[]{
                "branding/app-logo.png",
                "static/app-logo.png",
                "app-logo.png"
        };
        for (String path : candidates) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                return in.readAllBytes();
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private String buildAddressText(Address address) {
        if (address == null) {
            return "-";
        }
        List<String> parts = new ArrayList<>();
        if (address.getAddressLine1() != null && !address.getAddressLine1().isBlank()) parts.add(address.getAddressLine1());
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) parts.add(address.getAddressLine2());
        if (address.getLandmark() != null && !address.getLandmark().isBlank()) parts.add(address.getLandmark());
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) parts.add(address.getDistrict());
        if (address.getCity() != null && !address.getCity().isBlank()) parts.add(address.getCity());
        if (address.getState() != null && !address.getState().isBlank()) parts.add(address.getState());
        if (address.getPincode() != null && !address.getPincode().isBlank()) parts.add(address.getPincode());
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
