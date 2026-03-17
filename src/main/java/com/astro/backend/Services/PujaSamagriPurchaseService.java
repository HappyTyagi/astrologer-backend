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
        final String status = valueOrDefault(first.getStatus(), "COMPLETED");
        final double walletUsed = first.getWalletUsed() == null ? 0.0 : Math.max(0.0, first.getWalletUsed());
        final double gatewayPaid = first.getGatewayPaid() == null ? 0.0 : Math.max(0.0, first.getGatewayPaid());

        double subtotal = 0.0;
        double discount = 0.0;
        double total = 0.0;
        int totalQty = 0;
        int sr = 1;
        final StringBuilder rows = new StringBuilder();

        for (PujaSamagriPurchase item : purchases) {
            final int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            final double unitPrice = item.getUnitPrice() == null ? 0.0 : item.getUnitPrice();
            final double finalUnitPrice = item.getFinalUnitPrice() == null ? unitPrice : item.getFinalUnitPrice();
            final double line = item.getLineTotal() == null ? finalUnitPrice * quantity : item.getLineTotal();
            final double mrpLine = unitPrice * quantity;
            final double lineDiscount = Math.max(0.0, mrpLine - line);

            subtotal += mrpLine;
            discount += lineDiscount;
            total += line;
            totalQty += quantity;

            rows.append("""
                        <tr>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;">%d</td>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;font-weight:600;color:#2d2c2a;">%s</td>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;text-align:center;">%d</td>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;text-align:right;">%s</td>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;text-align:right;">%s</td>
                          <td style="padding:11px 10px;border-bottom:1px solid #ece8da;text-align:right;font-weight:700;color:#1f3d8a;">%s</td>
                        </tr>
                    """.formatted(
                    sr++,
                    escapeHtml(valueOrDefault(item.getName(), "Puja Samagri")),
                    quantity,
                    escapeHtml(formatMoney(unitPrice, currency)),
                    escapeHtml(formatMoney(lineDiscount, currency)),
                    escapeHtml(formatMoney(line, currency))
            ));
        }

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <style>
                    body { margin:0; background:#f5f2e9; font-family:Calibri, Arial, sans-serif; color:#1f1f1f; }
                    .wrap { max-width:840px; margin:24px auto; padding:0 14px; }
                    .card { background:#ffffff; border:1px solid #eee5d1; border-radius:22px; overflow:hidden; box-shadow:0 12px 28px rgba(40,35,18,.08); }
                    .hero { background:linear-gradient(135deg,#1e2237,#3c2d67); color:#ffffff; padding:24px; }
                    .hero h2 { margin:0; font-size:25px; letter-spacing:.2px; }
                    .hero p { margin:8px 0 0; opacity:.92; font-size:14px; }
                    .body { padding:22px; }
                    .meta { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; margin-bottom:14px; }
                    .chip { border:1px solid #ece4d2; border-radius:12px; background:#fcfaf4; padding:10px 12px; font-size:13px; }
                    .label { color:#7e7769; font-size:12px; text-transform:uppercase; letter-spacing:.4px; display:block; margin-bottom:3px; }
                    .value { color:#27251f; font-weight:700; }
                    .status { display:inline-block; background:#e7f7ed; color:#1f8b4a; font-weight:700; border-radius:999px; padding:5px 10px; font-size:12px; border:1px solid #bce5cb; }
                    table { width:100%%; border-collapse:collapse; margin-top:14px; border:1px solid #ece8da; border-radius:12px; overflow:hidden; }
                    thead th { background:#fbf8f1; color:#706956; font-size:12px; text-transform:uppercase; letter-spacing:.3px; padding:10px; border-bottom:1px solid #ece8da; text-align:left; }
                    .summary { margin:15px 0 0 auto; max-width:360px; border:1px solid #ece4d3; border-radius:14px; padding:14px; background:#fcfaf4; }
                    .sum-row { display:flex; justify-content:space-between; padding:6px 0; font-size:14px; color:#34312a; }
                    .sum-row.total { border-top:1px solid #e9e2d0; margin-top:8px; padding-top:10px; font-size:20px; font-weight:700; color:#1f3d8a; }
                    .sum-row .good { color:#1f8b4a; font-weight:700; }
                    .address { margin-top:16px; border:1px solid #ece4d3; border-radius:14px; background:#fcfaf4; padding:12px; font-size:13px; color:#464235; }
                    .foot { margin-top:14px; font-size:12px; color:#857f71; line-height:1.6; }
                    @media (max-width: 640px) { .meta { grid-template-columns:1fr; } }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="hero">
                        <h2>Puja Samagri Receipt</h2>
                        <p>Thank you for shopping with Astro Adhyaay. Your premium PDF invoice is attached.</p>
                      </div>
                      <div class="body">
                        <div class="meta">
                          <div class="chip"><span class="label">Order ID</span><span class="value">%s</span></div>
                          <div class="chip"><span class="label">Order Date</span><span class="value">%s</span></div>
                          <div class="chip"><span class="label">Payment Method</span><span class="value">%s</span></div>
                          <div class="chip"><span class="label">Transaction</span><span class="value">%s</span></div>
                        </div>

                        <div style="margin:2px 0 6px;">
                          <span class="label" style="display:inline;color:#7e7769;">Status:</span>
                          <span class="status">%s</span>
                        </div>

                        <table>
                          <thead>
                            <tr>
                              <th style="width:7%%;">#</th>
                              <th style="width:35%%;">Item</th>
                              <th style="width:9%%;text-align:center;">Qty</th>
                              <th style="width:16%%;text-align:right;">MRP</th>
                              <th style="width:13%%;text-align:right;">Discount</th>
                              <th style="width:20%%;text-align:right;">Amount</th>
                            </tr>
                          </thead>
                          <tbody>%s</tbody>
                        </table>

                        <div class="summary">
                          <div class="sum-row"><span>Total Items</span><span>%d</span></div>
                          <div class="sum-row"><span>Subtotal</span><span>%s</span></div>
                          <div class="sum-row"><span>Discount</span><span class="good">-%s</span></div>
                          <div class="sum-row"><span>Wallet Used</span><span>%s</span></div>
                          <div class="sum-row"><span>Gateway Paid</span><span>%s</span></div>
                          <div class="sum-row total"><span>Grand Total</span><span>%s</span></div>
                        </div>

                        <div class="address"><strong>Delivery Address:</strong><br/>%s</div>
                        <div class="foot">
                          Need support? Reply to this email and our team will assist you.<br/>
                          Astrologer Services Private Limited | support@astrologer.app
                        </div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(orderCode),
                escapeHtml(orderDate),
                escapeHtml(paymentMethod),
                escapeHtml(transactionId),
                escapeHtml(status),
                rows.toString(),
                totalQty,
                escapeHtml(formatMoney(subtotal, currency)),
                escapeHtml(formatMoney(discount, currency)),
                escapeHtml(formatMoney(walletUsed, currency)),
                escapeHtml(formatMoney(gatewayPaid, currency)),
                escapeHtml(formatMoney(total, currency)),
                escapeHtml(addressText)
        );
    }

    private byte[] buildReceiptPdf(List<PujaSamagriPurchase> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            return new byte[0];
        }

        final PujaSamagriPurchase first = purchases.get(0);
        final String currency = valueOrDefault(first.getCurrency(), "INR");
        final double walletUsed = first.getWalletUsed() == null ? 0.0 : Math.max(0.0, first.getWalletUsed());
        final double gatewayPaid = first.getGatewayPaid() == null ? 0.0 : Math.max(0.0, first.getGatewayPaid());

        double subtotal = 0.0;
        double total = 0.0;
        double discount = 0.0;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            FontFactory.registerDirectories();
            Font heroTitle = invoiceFont(21, Font.BOLD, Color.WHITE);
            Font heroSub = invoiceFont(10.5f, Font.NORMAL, new Color(224, 232, 255));
            Font panelLabel = invoiceFont(9.2f, Font.BOLD, new Color(94, 102, 118));
            Font panelValue = invoiceFont(10.7f, Font.BOLD, new Color(31, 43, 67));
            Font detailTitle = invoiceFont(10.3f, Font.BOLD, new Color(26, 43, 94));
            Font detailBody = invoiceFont(9.9f, Font.NORMAL, new Color(34, 40, 54));
            Font tableHead = invoiceFont(9.8f, Font.BOLD, Color.WHITE);
            Font tableBody = invoiceFont(9.8f, Font.NORMAL, new Color(30, 36, 52));
            Font totalFont = invoiceFont(12.7f, Font.BOLD, new Color(23, 48, 122));
            Font footFont = invoiceFont(9.2f, Font.NORMAL, new Color(86, 94, 108));

            Color heroBg = new Color(30, 37, 73);
            Color panelBg = new Color(248, 250, 255);
            Color panelBorder = new Color(220, 228, 244);
            Color tableHeaderBg = new Color(44, 64, 136);

            PdfPTable hero = new PdfPTable(1);
            hero.setWidthPercentage(100f);
            PdfPCell heroCell = new PdfPCell();
            heroCell.setBackgroundColor(heroBg);
            heroCell.setBorderColor(heroBg);
            heroCell.setPadding(16f);
            Paragraph title = new Paragraph("Puja Samagri Tax Invoice", heroTitle);
            title.setSpacingAfter(5f);
            heroCell.addElement(title);
            heroCell.addElement(new Paragraph("Astrologer Services Private Limited", heroSub));
            heroCell.addElement(new Paragraph("Original receipt for customer", heroSub));
            hero.addCell(heroCell);
            hero.setSpacingAfter(9f);
            document.add(hero);

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100f);
            meta.setSpacingAfter(10f);
            meta.addCell(invoicePanelCell("Invoice No", shortOrderId(first.getOrderId()), panelLabel, panelValue, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Invoice Date", formatDateTime(first.getPurchasedAt()), panelLabel, panelValue, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Payment", valueOrDefault(first.getPaymentMethod(), "NA"), panelLabel, panelValue, panelBg, panelBorder));
            meta.addCell(invoicePanelCell("Status", valueOrDefault(first.getStatus(), "COMPLETED"), panelLabel, panelValue, panelBg, panelBorder));
            document.add(meta);

            PdfPTable details = new PdfPTable(new float[]{1f, 1f});
            details.setWidthPercentage(100f);
            details.setSpacingAfter(10f);
            details.addCell(invoiceDetailCell(
                    "Delivery Address",
                    new String[]{
                            formatAddress(first.getAddress()),
                            "Customer ID: " + valueOrDefault(first.getUserId() == null ? null : String.valueOf(first.getUserId()), "-")
                    },
                    detailTitle,
                    detailBody,
                    panelBg,
                    panelBorder
            ));
            details.addCell(invoiceDetailCell(
                    "Payment Details",
                    new String[]{
                            "Transaction: " + valueOrDefault(first.getTransactionId(), "NA"),
                            "Wallet Used: " + formatMoney(walletUsed, currency),
                            "Gateway Paid: " + formatMoney(gatewayPaid, currency)
                    },
                    detailTitle,
                    detailBody,
                    panelBg,
                    panelBorder
            ));
            document.add(details);

            PdfPTable table = new PdfPTable(new float[]{0.8f, 3.5f, 0.9f, 1.5f, 1.2f, 1.5f});
            table.setWidthPercentage(100f);
            table.setSpacingAfter(9f);
            table.addCell(invoiceTableHeaderCell("#", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Item", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Qty", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("MRP", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Disc.", tableHead, tableHeaderBg));
            table.addCell(invoiceTableHeaderCell("Amount", tableHead, tableHeaderBg));

            int sr = 1;
            for (PujaSamagriPurchase item : purchases) {
                final int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
                final double unitPrice = item.getUnitPrice() == null ? 0.0 : item.getUnitPrice();
                final double finalUnitPrice = item.getFinalUnitPrice() == null ? unitPrice : item.getFinalUnitPrice();
                final double line = item.getLineTotal() == null ? finalUnitPrice * quantity : item.getLineTotal();
                final double mrpLine = unitPrice * quantity;
                final double lineDiscount = Math.max(0.0, mrpLine - line);
                subtotal += mrpLine;
                total += line;
                discount += lineDiscount;

                table.addCell(invoiceTableBodyCell(String.valueOf(sr++), tableBody, Element.ALIGN_CENTER, Color.WHITE, panelBorder));
                table.addCell(invoiceTableBodyCell(valueOrDefault(item.getName(), "Puja Samagri"), tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
                table.addCell(invoiceTableBodyCell(String.valueOf(quantity), tableBody, Element.ALIGN_CENTER, Color.WHITE, panelBorder));
                table.addCell(invoiceTableBodyCell(formatMoney(unitPrice, currency), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
                table.addCell(invoiceTableBodyCell(formatMoney(lineDiscount, currency), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
                table.addCell(invoiceTableBodyCell(formatMoney(line, currency), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            }
            document.add(table);

            PdfPTable summary = new PdfPTable(new float[]{1.7f, 1f});
            summary.setWidthPercentage(45f);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summary.setSpacingAfter(10f);
            summary.addCell(invoiceTableBodyCell("Subtotal", detailBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            summary.addCell(invoiceTableBodyCell(formatMoney(subtotal, currency), detailBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            summary.addCell(invoiceTableBodyCell("Discount", detailBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            summary.addCell(invoiceTableBodyCell("-" + formatMoney(discount, currency), detailBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            summary.addCell(invoiceTableBodyCell("Wallet Used", detailBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            summary.addCell(invoiceTableBodyCell(formatMoney(walletUsed, currency), detailBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            summary.addCell(invoiceTableBodyCell("Gateway Paid", detailBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            summary.addCell(invoiceTableBodyCell(formatMoney(gatewayPaid, currency), detailBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            summary.addCell(invoiceTableBodyCell("Grand Total", totalFont, Element.ALIGN_LEFT, new Color(231, 238, 255), panelBorder));
            summary.addCell(invoiceTableBodyCell(formatMoney(total, currency), totalFont, Element.ALIGN_RIGHT, new Color(231, 238, 255), panelBorder));
            document.add(summary);

            Paragraph help = new Paragraph("Need help? support@astrologer.app", footFont);
            help.setSpacingAfter(3f);
            document.add(help);
            document.add(new Paragraph("This is a system-generated invoice and does not require signature.", footFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            return new byte[0];
        }
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
            Color bgColor,
            Color borderColor
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(9f);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(borderColor);
        Paragraph labelPara = new Paragraph(valueOrDefault(label, "-"), labelFont);
        labelPara.setSpacingAfter(4f);
        cell.addElement(labelPara);
        cell.addElement(new Paragraph(valueOrDefault(value, "-"), valueFont));
        return cell;
    }

    private PdfPCell invoiceDetailCell(
            String title,
            String[] lines,
            Font titleFont,
            Font bodyFont,
            Color bgColor,
            Color borderColor
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(borderColor);
        Paragraph titlePara = new Paragraph(valueOrDefault(title, "-"), titleFont);
        titlePara.setSpacingAfter(5f);
        cell.addElement(titlePara);
        if (lines != null) {
            for (String line : lines) {
                cell.addElement(new Paragraph(valueOrDefault(line, "-"), bodyFont));
            }
        }
        return cell;
    }

    private PdfPCell invoiceTableHeaderCell(String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDefault(text, "-"), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7f);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(bgColor);
        return cell;
    }

    private PdfPCell invoiceTableBodyCell(String text, Font font, int align, Color bgColor, Color borderColor) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDefault(text, "-"), font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6.5f);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(borderColor);
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
