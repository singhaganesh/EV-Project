package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.ChargingSession;
import com.ganesh.EV_Project.model.Payment;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders a customer payment receipt as a PDF.
 *
 * Intentionally a *payment receipt*, not a GST tax invoice — the company is not
 * yet GST-registered. The layout is structured so that adding GSTIN, a tax
 * breakup and an HSN/SAC code later (to make it a compliant tax invoice) is a
 * small change here, not a rewrite.
 *
 * Amounts are printed as "Rs." rather than the ₹ glyph because the base-14 PDF
 * fonts don't carry U+20B9; switch to an embedded TTF if the symbol is required.
 */
@Service
public class ReceiptService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Color BRAND = new Color(0x00, 0xBC, 0xD4);
    private static final Color DARK = new Color(0x1A, 0x22, 0x34);

    public byte[] generate(ChargingSession session, Payment payment) {
        Booking booking = session.getBooking();
        User user = booking != null ? booking.getUser() : null;
        ChargerSlot slot = booking != null ? booking.getSlot() : null;
        Station station = slot != null ? slot.getStation() : null;

        double energy = session.getEnergyKwh() != null ? session.getEnergyKwh() : 0.0;
        double total = session.getTotalCost() != null ? session.getTotalCost() : 0.0;
        double rate = energy > 0 ? total / energy : 0.0;

        LocalDateTime when = payment != null && payment.getPaidAt() != null
                ? payment.getPaidAt() : session.getEndTime();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Header ──
            doc.add(new Paragraph("Plugsy", new Font(Font.HELVETICA, 24, Font.BOLD, BRAND)));
            doc.add(new Paragraph("Payment Receipt", new Font(Font.HELVETICA, 12, Font.NORMAL, DARK)));
            Paragraph note = new Paragraph("This is a payment receipt, not a tax invoice.",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
            note.setSpacingAfter(14f);
            doc.add(note);

            keyVals(doc,
                    new String[]{"Receipt No.", "Date"},
                    new String[]{"PLG-" + session.getId(), when != null ? when.format(DTF) : "-"});

            // ── Billed To ──
            sectionTitle(doc, "Billed To");
            keyVals(doc,
                    new String[]{"Name", "Mobile"},
                    new String[]{
                            user != null && user.getName() != null ? user.getName() : "-",
                            user != null && user.getMobileNumber() != null ? user.getMobileNumber() : "-"});

            // ── Station ──
            sectionTitle(doc, "Charging Station");
            keyVals(doc,
                    new String[]{"Station", "Address", "Connector"},
                    new String[]{
                            station != null ? station.getName() : "-",
                            station != null ? station.getAddress() : "-",
                            slot != null
                                    ? slot.getSlotType() + " | " + slot.getConnectorType() + " | " + kw(slot.getPowerKw())
                                    : "-"});

            // ── Session ──
            sectionTitle(doc, "Session");
            keyVals(doc,
                    new String[]{"Session ID", "Start", "End", "Energy", "Rate"},
                    new String[]{
                            "#" + session.getId(),
                            dt(session.getStartTime()),
                            dt(session.getEndTime()),
                            String.format("%.2f kWh", energy),
                            "Rs. " + String.format("%.2f", rate) + " / kWh"});

            // ── Total ──
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(100);
            totals.setSpacingBefore(12f);
            PdfPCell tl = new PdfPCell(new Phrase("Total Paid", new Font(Font.HELVETICA, 12, Font.BOLD, DARK)));
            PdfPCell tr = new PdfPCell(new Phrase("Rs. " + String.format("%.2f", total),
                    new Font(Font.HELVETICA, 16, Font.BOLD, BRAND)));
            for (PdfPCell c : new PdfPCell[]{tl, tr}) {
                c.setBorder(Rectangle.TOP);
                c.setBorderColor(Color.LIGHT_GRAY);
                c.setPadding(8f);
            }
            tr.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.addCell(tl);
            totals.addCell(tr);
            doc.add(totals);

            // ── Payment ──
            sectionTitle(doc, "Payment");
            String method = payment != null && payment.getPaymentMethod() != null
                    ? payment.getPaymentMethod()
                    : (payment != null && payment.getGateway() != null ? payment.getGateway() : "Razorpay");
            String txn = payment != null && payment.getTransactionId() != null
                    ? payment.getTransactionId()
                    : (session.getRazorpayOrderId() != null ? session.getRazorpayOrderId() : "-");
            keyVals(doc,
                    new String[]{"Method", "Transaction ID", "Status", "Paid At"},
                    new String[]{method, txn, "PAID", when != null ? when.format(DTF) : "-"});

            // ── Footer ──
            Paragraph footer = new Paragraph(
                    "\nThank you for charging with Plugsy.\nQuestions? support@plugsy.in",
                    new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY));
            footer.setSpacingBefore(24f);
            doc.add(footer);

            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
        return out.toByteArray();
    }

    private static String dt(LocalDateTime t) {
        return t != null ? t.format(DTF) : "-";
    }

    private static String kw(Double powerKw) {
        return powerKw != null ? Math.round(powerKw) + " kW" : "-";
    }

    private static void sectionTitle(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, new Font(Font.HELVETICA, 11, Font.BOLD, DARK));
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        doc.add(p);
    }

    private static void keyVals(Document doc, String[] keys, String[] values) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        Font kf = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        Font vf = new Font(Font.HELVETICA, 10, Font.NORMAL, DARK);
        for (int i = 0; i < keys.length; i++) {
            PdfPCell k = new PdfPCell(new Phrase(keys[i], kf));
            PdfPCell v = new PdfPCell(new Phrase(values[i] != null ? values[i] : "-", vf));
            k.setBorder(Rectangle.NO_BORDER);
            v.setBorder(Rectangle.NO_BORDER);
            k.setPadding(3f);
            v.setPadding(3f);
            v.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(k);
            t.addCell(v);
        }
        doc.add(t);
    }
}
