package com.astro.backend.Services;


import com.astro.backend.Entity.Address;
import com.astro.backend.Entity.MobileUserProfile;
import com.astro.backend.Entity.Wallet;
import com.astro.backend.Entity.WalletTransaction;
import com.astro.backend.Repositry.AddressRepository;
import com.astro.backend.Repositry.MobileUserProfileRepository;
import com.astro.backend.Repositry.WalletRepository;
import com.astro.backend.Repositry.WalletTransactionRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;
    private final MobileUserProfileRepository mobileUserProfileRepository;
    private final AddressRepository addressRepository;
    private final EmailService emailService;

    public Wallet getWallet(Long userId) {
        return walletRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet w = Wallet.builder()
                            .userId(userId)
                            .balance(0)
                            .cashback(0.0)
                            .bonus(0.0)
                            .build();
                    return walletRepo.save(w);
                });
    }

    public WalletTransaction credit(Long userId, double amount, String ref, String desc) {
        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepo.save(wallet);

        WalletTransaction transaction = txnRepo.save(WalletTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type("CREDIT")
                .refId(ref)
                .description(desc)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build());
        sendWalletTopupReceiptAsync(userId, transaction, wallet.getBalance());
        return transaction;
    }

    public boolean debit(Long userId, double amount, String ref, String desc) {
        Wallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) return false;

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepo.save(wallet);

        txnRepo.save(WalletTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .type("DEBIT")
                .refId(ref)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build());

        return true;
    }

    public double debitUpTo(Long userId, double requestedAmount, String ref, String desc) {
        if (requestedAmount <= 0) return 0.0;
        Wallet wallet = getWallet(userId);
        double currentBalance = wallet.getBalance();
        double toDebit = Math.min(currentBalance, requestedAmount);
        if (toDebit <= 0) return 0.0;

        wallet.setBalance(currentBalance - toDebit);
        walletRepo.save(wallet);

        txnRepo.save(WalletTransaction.builder()
                .userId(userId)
                .amount(-toDebit)
                .type("DEBIT")
                .refId(ref)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build());

        return toDebit;
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        return txnRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void sendWalletTopupReceiptAsync(Long userId, WalletTransaction transaction, double balanceAfter) {
        try {
            MobileUserProfile profile = mobileUserProfileRepository.findByUserId(userId).orElse(null);
            String toEmail = profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim();
            if (toEmail.isEmpty()) {
                return;
            }

            Address address = findPrimaryAddress(profile);
            String subject = "Wallet Top-up Receipt - " + valueOrDash(transaction.getRefId());
            String html = buildWalletTopupEmailHtml(profile, transaction, balanceAfter, address);
            byte[] pdf = buildWalletTopupPdf(profile, transaction, balanceAfter, address);
            emailService.sendEmailWithAttachmentAsync(
                    toEmail,
                    subject,
                    html,
                    "wallet-topup-invoice.pdf",
                    pdf,
                    "application/pdf"
            );
        } catch (Exception ignored) {
            // Top-up flow should not fail if email receipt cannot be queued.
        }
    }

    private Address findPrimaryAddress(MobileUserProfile profile) {
        if (profile == null || profile.getMobileNumber() == null || profile.getMobileNumber().isBlank()) {
            return null;
        }
        List<Address> list = addressRepository.findByUserMobileNumber(profile.getMobileNumber().trim());
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (Address a : list) {
            if (Boolean.TRUE.equals(a.getIsDefault())) {
                return a;
            }
        }
        return list.get(0);
    }

    private String buildWalletTopupEmailHtml(
            MobileUserProfile profile,
            WalletTransaction transaction,
            double balanceAfter,
            Address address
    ) {
        String txnDate = formatDateTime(transaction.getCreatedAt());
        String amount = formatMoney(transaction.getAmount(), "INR");
        String ref = valueOrDash(transaction.getRefId());
        String name = profile == null ? "-" : valueOrDash(profile.getName());
        String mobile = profile == null ? "-" : valueOrDash(profile.getMobileNumber());
        String mail = profile == null ? "-" : valueOrDash(profile.getEmail());

        return """
                <html>
                <body style="margin:0;padding:0;background:#f4f7fd;font-family:Calibri,Arial,sans-serif;color:#222a3a;">
                  <div style="max-width:700px;margin:0 auto;padding:20px 14px;">
                    <div style="background:#ffffff;border:1px solid #e3e8f3;border-radius:14px;overflow:hidden;">
                      <div style="padding:18px 20px;background:linear-gradient(90deg,#1f2f73,#3247a9);color:#ffffff;">
                        <h2 style="margin:0;font-size:22px;">Wallet Top-up Successful</h2>
                        <p style="margin:8px 0 0 0;font-size:13px;color:#dce3ff;">Payment receipt and invoice details are below.</p>
                      </div>
                      <div style="padding:18px 20px;">
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr><td style="padding:9px 0;color:#5e6679;">Reference ID</td><td style="padding:9px 0;text-align:right;"><strong>%s</strong></td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Date</td><td style="padding:9px 0;text-align:right;">%s</td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Amount Added</td><td style="padding:9px 0;text-align:right;"><strong>%s</strong></td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Wallet Balance</td><td style="padding:9px 0;text-align:right;"><strong>%s</strong></td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Name</td><td style="padding:9px 0;text-align:right;">%s</td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Mobile</td><td style="padding:9px 0;text-align:right;">%s</td></tr>
                          <tr><td style="padding:9px 0;color:#5e6679;">Email</td><td style="padding:9px 0;text-align:right;">%s</td></tr>
                        </table>
                        <div style="margin-top:12px;padding:12px;background:#f8fbff;border:1px solid #dfe7f5;border-radius:10px;">
                          <p style="margin:0;color:#4b5874;font-size:13px;"><strong>Address:</strong> %s</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(ref),
                escapeHtml(txnDate),
                escapeHtml(amount),
                escapeHtml(formatMoney(balanceAfter, "INR")),
                escapeHtml(name),
                escapeHtml(mobile),
                escapeHtml(mail),
                escapeHtml(buildAddressText(address))
        );
    }

    private byte[] buildWalletTopupPdf(
            MobileUserProfile profile,
            WalletTransaction transaction,
            double balanceAfter,
            Address address
    ) {
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
            Font totalFont = invoiceFont(12.5f, Font.BOLD, new Color(20, 36, 90));

            Color heroBg = new Color(24, 42, 96);
            Color panelBg = new Color(246, 249, 255);
            Color panelBorder = new Color(222, 231, 246);
            Color tableHeaderBg = new Color(45, 66, 137);

            String referenceId = valueOrDash(transaction.getRefId());
            String invoiceDate = formatDate(transaction.getCreatedAt());
            String amount = formatMoney(transaction.getAmount(), "INR");
            String userName = valueOrDash(profile == null ? null : profile.getName());
            String userId = valueOrDash(profile == null ? null : String.valueOf(profile.getUserId()));
            String mobile = valueOrDash(profile == null ? null : profile.getMobileNumber());
            String email = valueOrDash(profile == null ? null : profile.getEmail());
            String addressText = buildAddressText(address);
            String balanceText = formatMoney(balanceAfter, "INR");

            PdfPTable hero = new PdfPTable(1);
            hero.setWidthPercentage(100);
            PdfPCell heroCell = new PdfPCell();
            heroCell.setBackgroundColor(heroBg);
            heroCell.setBorderColor(heroBg);
            heroCell.setPadding(16f);
            Paragraph heroTitleP = new Paragraph("Tax Invoice", heroTitle);
            heroTitleP.setSpacingAfter(5f);
            heroCell.addElement(heroTitleP);
            heroCell.addElement(new Paragraph("Wallet Top-up Receipt", invoiceFont(14, Font.BOLD, Color.WHITE)));
            heroCell.addElement(new Paragraph("ORIGINAL FOR RECIPIENT", heroSub));
            hero.addCell(heroCell);
            hero.setSpacingAfter(10f);
            document.add(hero);

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(10f);
            meta.addCell(panelCell("Invoice No", referenceId, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(panelCell("Invoice Date", invoiceDate, labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(panelCell("Payment Mode", "Gateway", labelFont, valueFont, panelBg, panelBorder));
            meta.addCell(panelCell("Status", "Successful", labelFont, valueFont, panelBg, panelBorder));
            document.add(meta);

            PdfPTable details = new PdfPTable(new float[]{1f, 1f});
            details.setWidthPercentage(100);
            details.setSpacingAfter(10f);
            details.addCell(detailBlock(
                    "Billed To",
                    new String[]{
                            "Name: " + userName,
                            "User ID: " + userId,
                            "Mobile: " + mobile,
                            "Email: " + email,
                            "Address: " + addressText
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            details.addCell(detailBlock(
                    "Company Details",
                    new String[]{
                            "ASTROLOGER SERVICES PRIVATE LIMITED",
                            "GSTIN: 09ASTRO1234X1Z9",
                            "Email: support@astrologer.app",
                            "Service: Wallet top-up / recharge",
                            "Region: India"
                    },
                    sectionTitle,
                    valueFont,
                    panelBg,
                    panelBorder
            ));
            document.add(details);

            PdfPTable amountTable = new PdfPTable(new float[]{1.1f, 5.8f, 2.1f});
            amountTable.setWidthPercentage(100);
            amountTable.setSpacingAfter(10f);
            amountTable.addCell(tableHeaderCell("Sr. No", tableHead, tableHeaderBg));
            amountTable.addCell(tableHeaderCell("Particulars", tableHead, tableHeaderBg));
            amountTable.addCell(tableHeaderCell("Amount", tableHead, tableHeaderBg));
            amountTable.addCell(tableTextCell("1", tableBody, Element.ALIGN_CENTER, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell("Wallet recharge credited to user wallet", tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell(amount, tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell("", tableBody, Element.ALIGN_LEFT, panelBg, panelBorder));
            amountTable.addCell(tableTextCell("Taxable Amount", tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            amountTable.addCell(tableTextCell(amount, tableBody, Element.ALIGN_RIGHT, panelBg, panelBorder));
            amountTable.addCell(tableTextCell("", tableBody, Element.ALIGN_LEFT, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell("IGST @ 0.00%", tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell(formatMoney(0.0, "INR"), tableBody, Element.ALIGN_RIGHT, Color.WHITE, panelBorder));
            amountTable.addCell(tableTextCell("", tableBody, Element.ALIGN_LEFT, new Color(232, 239, 255), panelBorder));
            amountTable.addCell(tableTextCell("Total Amount", totalFont, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));
            amountTable.addCell(tableTextCell(amount, totalFont, Element.ALIGN_RIGHT, new Color(232, 239, 255), panelBorder));
            document.add(amountTable);

            PdfPTable summary = new PdfPTable(new float[]{2.7f, 1.3f});
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(10f);
            PdfPCell summaryLabel = new PdfPCell(new Phrase("Wallet Balance After Top-up", invoiceFont(11, Font.BOLD, new Color(19, 40, 95))));
            summaryLabel.setPadding(10f);
            summaryLabel.setBorderColor(new Color(186, 205, 242));
            summaryLabel.setBackgroundColor(new Color(241, 246, 255));
            summary.addCell(summaryLabel);
            PdfPCell summaryValue = new PdfPCell(new Phrase(balanceText, invoiceFont(12, Font.BOLD, new Color(19, 40, 95))));
            summaryValue.setPadding(10f);
            summaryValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryValue.setBorderColor(new Color(186, 205, 242));
            summaryValue.setBackgroundColor(new Color(241, 246, 255));
            summary.addCell(summaryValue);
            document.add(summary);

            Paragraph note = new Paragraph("This is a computer generated invoice and does not require signature.", invoiceFont(9.5f, Font.NORMAL, new Color(84, 93, 112)));
            note.setSpacingAfter(3f);
            document.add(note);
            document.add(new Paragraph("For support: support@astrologer.app", invoiceFont(9.5f, Font.NORMAL, new Color(84, 93, 112))));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate wallet top-up invoice PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell panelCell(
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

    private PdfPCell detailBlock(
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

    private PdfPCell tableHeaderCell(String value, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDash(value), font));
        cell.setPadding(8f);
        cell.setBorderColor(background);
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment("Amount".equalsIgnoreCase(value) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell tableTextCell(String value, Font font, int align, Color background, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDash(value), font));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(border);
        cell.setBackgroundColor(background);
        return cell;
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, invoiceFont(11, Font.BOLD, Color.BLACK)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(238, 238, 238));
        cell.setPadding(8f);
        cell.setBorderColor(Color.BLACK);
        cell.setBorderWidth(1.2f);
        return cell;
    }

    private PdfPCell bodyCell(String value, int align, Font font, float borderWidth) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, font));
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

    private String formatDateTime(LocalDateTime value) {
        if (value == null) return "-";
        return value.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) return "-";
        return value.toLocalDate().toString();
    }

    private String formatMoney(double amount, String currency) {
        String c = currency == null ? "INR" : currency.toUpperCase(Locale.ROOT);
        String symbol = "INR".equals(c) ? "INR " : c + " ";
        return symbol + String.format(Locale.US, "%.2f", amount);
    }

    private String buildAddressText(Address address) {
        if (address == null) return "-";
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

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
