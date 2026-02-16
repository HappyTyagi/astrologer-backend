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
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
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
            PdfWriter.getInstance(document, out);
            document.open();

            FontFactory.registerDirectories();
            Font titleFont = invoiceFont(30, Font.BOLD, Color.BLACK);
            Font subTitleFont = invoiceFont(15, Font.BOLD, Color.BLACK);
            Font sectionFont = invoiceFont(12, Font.BOLD, Color.BLACK);
            Font bodyFont = invoiceFont(11, Font.NORMAL, Color.BLACK);
            Font boldFont = invoiceFont(11, Font.BOLD, Color.BLACK);
            Font totalFont = invoiceFont(13, Font.BOLD, Color.BLACK);

            Paragraph title = new Paragraph("Tax Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(2f);
            document.add(title);
            Paragraph subTitle = new Paragraph("ORIGINAL FOR RECIPIENT", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(14f);
            document.add(subTitle);

            document.add(new Paragraph("ASTROLOGER SERVICES PRIVATE LIMITED", sectionFont));
            document.add(new Paragraph("Address: Digital Astrology Services, India", bodyFont));
            document.add(new Paragraph("GSTIN: 09ASTRO1234X1Z9    Email: support@astrologer.app", bodyFont));
            document.add(new Paragraph("Invoice No: " + valueOrDash(transaction.getRefId()), boldFont));
            document.add(new Paragraph("Invoice Date: " + formatDate(transaction.getCreatedAt()), boldFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("User Details", sectionFont));
            document.add(new Paragraph("Name: " + valueOrDash(profile == null ? null : profile.getName()), bodyFont));
            document.add(new Paragraph("User ID: " + valueOrDash(profile == null ? null : String.valueOf(profile.getUserId())), bodyFont));
            document.add(new Paragraph("Mobile: " + valueOrDash(profile == null ? null : profile.getMobileNumber()), bodyFont));
            document.add(new Paragraph("Email: " + valueOrDash(profile == null ? null : profile.getEmail()), bodyFont));
            document.add(new Paragraph("Address: " + buildAddressText(address), bodyFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Service Details", sectionFont));
            document.add(new Paragraph("Service Description: Wallet top-up / recharge", bodyFont));
            document.add(new Paragraph("Work Description:", bodyFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{1.1f, 6.5f, 1.9f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(8f);
            table.addCell(headerCell("Sr. No"));
            table.addCell(headerCell("Particulars"));
            table.addCell(headerCell("Amount (Rs.)"));

            table.addCell(bodyCell("", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("Reference ID: " + valueOrDash(transaction.getRefId()), Element.ALIGN_LEFT, boldFont, 1f));
            table.addCell(bodyCell("", Element.ALIGN_RIGHT, bodyFont, 1f));
            table.addCell(bodyCell("", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("Order Date: " + formatDate(transaction.getCreatedAt()), Element.ALIGN_LEFT, boldFont, 1f));
            table.addCell(bodyCell("", Element.ALIGN_RIGHT, bodyFont, 1f));

            table.addCell(bodyCell("1", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("Wallet recharge credited to user wallet", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell(formatMoney(transaction.getAmount(), "INR"), Element.ALIGN_RIGHT, bodyFont, 1f));

            table.addCell(bodyCell("", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("Taxable Amount", Element.ALIGN_RIGHT, bodyFont, 1f));
            table.addCell(bodyCell(formatMoney(transaction.getAmount(), "INR"), Element.ALIGN_RIGHT, bodyFont, 1f));
            table.addCell(bodyCell("", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("IGST @ 0.00%", Element.ALIGN_RIGHT, bodyFont, 1f));
            table.addCell(bodyCell(formatMoney(0.0, "INR"), Element.ALIGN_RIGHT, bodyFont, 1f));
            table.addCell(bodyCell("", Element.ALIGN_LEFT, bodyFont, 1f));
            table.addCell(bodyCell("Total Amount", Element.ALIGN_RIGHT, totalFont, 1.3f));
            table.addCell(bodyCell(formatMoney(transaction.getAmount(), "INR"), Element.ALIGN_RIGHT, totalFont, 1.3f));

            document.add(table);
            document.add(new Paragraph("Wallet Balance After Top-up: " + formatMoney(balanceAfter, "INR"), sectionFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("This is a computer generated invoice and does not require signature.", bodyFont));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate wallet top-up invoice PDF: " + e.getMessage(), e);
        }
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
