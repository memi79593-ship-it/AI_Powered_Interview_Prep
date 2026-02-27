package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.dto.DashboardDTO;
import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Day 11 – PDF Report Generation Service.
 *
 * Generates a downloadable PDF performance report for a user.
 * Includes: name/email, session history, score summary, weak areas, roadmap.
 *
 * Uses iTextPDF 5.x (open source, BSD license).
 */
@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    // ── Fonts ─────────────────────────────────────────────────────
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,
            new BaseColor(33, 97, 140));
    private static final Font HEADING_FONT = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,
            new BaseColor(44, 62, 80));
    private static final Font BODY_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.BLACK);
    private static final Font WEAK_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL,
            new BaseColor(192, 57, 43));
    private static final Font GOOD_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL,
            new BaseColor(39, 174, 96));

    private final InterviewSessionRepository sessionRepo;
    private final ScoreService scoreService;

    public PdfReportService(InterviewSessionRepository sessionRepo,
            ScoreService scoreService) {
        this.sessionRepo = sessionRepo;
        this.scoreService = scoreService;
    }

    /**
     * Generate a PDF report for a user.
     *
     * @param email User's email
     * @return PDF as byte array (ready for HTTP response)
     */
    public byte[] generateReport(String email) {
        log.info("Generating PDF report for: {}", email);

        DashboardDTO dashboard = scoreService.buildDashboard(email);
        List<InterviewSession> sessions = sessionRepo.findByUserEmailOrderByCreatedAtDesc(email);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, email, dashboard);
            addSummaryTable(doc, dashboard);
            addWeakAreasSection(doc, dashboard);
            addSessionHistory(doc, sessions);
            addFooter(doc);

            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }

        log.info("PDF report generated: {} bytes", out.size());
        return out.toByteArray();
    }

    // ── PDF Sections ─────────────────────────────────────────────

    private void addHeader(Document doc, String email, DashboardDTO d) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("AI Interview Performance Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        // Subtitle
        Paragraph sub = new Paragraph("AI-Powered Interview Prep Platform", BODY_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(6);
        doc.add(sub);

        // Date generated
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        Paragraph datePara = new Paragraph("Report generated: " + date, BODY_FONT);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(20);
        doc.add(datePara);

        // Horizontal rule
        LineSeparator line = new LineSeparator();
        line.setLineColor(new BaseColor(33, 97, 140));
        doc.add(new Chunk(line));

        // Email
        Paragraph emailPara = new Paragraph("\nCandidate: " + email, HEADING_FONT);
        emailPara.setSpacingAfter(5);
        doc.add(emailPara);
    }

    private void addSummaryTable(Document doc, DashboardDTO d) throws DocumentException {
        Paragraph heading = new Paragraph("\nPerformance Summary", HEADING_FONT);
        heading.setSpacingAfter(10);
        doc.add(heading);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(70);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setSpacingAfter(15);

        addRow(table, "Total Sessions", String.valueOf(d.getTotalSessions()));
        addRow(table, "Average Score", String.format("%.1f", d.getAverageScore()));
        addRow(table, "Highest Score", String.valueOf(d.getHighestScore()));
        addRow(table, "Best Role", d.getBestRole() != null ? d.getBestRole() : "N/A");
        addRow(table, "Weakest Role", d.getWeakestRole() != null ? d.getWeakestRole() : "N/A");
        addRow(table, "Last Interview",
                d.getLastInterviewDate() != null
                        ? d.getLastInterviewDate().substring(0, Math.min(10, d.getLastInterviewDate().length()))
                        : "N/A");

        doc.add(table);
    }

    private void addWeakAreasSection(Document doc, DashboardDTO d) throws DocumentException {
        Paragraph heading = new Paragraph("Weak Areas (Score < 50%)", HEADING_FONT);
        heading.setSpacingAfter(8);
        doc.add(heading);

        if (d.getWeakAreas() == null || d.getWeakAreas().isEmpty()) {
            doc.add(new Paragraph("✓ No weak areas detected. Great job!", GOOD_FONT));
        } else {
            for (String area : d.getWeakAreas()) {
                doc.add(new Paragraph("  ✗ " + area, WEAK_FONT));
            }
        }
        doc.add(Chunk.NEWLINE);

        // Role-wise breakdown
        if (d.getRoleWiseAverage() != null && !d.getRoleWiseAverage().isEmpty()) {
            Paragraph bk = new Paragraph("\nRole-wise Average Score", HEADING_FONT);
            bk.setSpacingAfter(8);
            doc.add(bk);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.setSpacingAfter(15);

            d.getRoleWiseAverage().forEach((role, avg) -> {
                try {
                    addRow(table, role, String.format("%.1f", avg));
                } catch (Exception ignored) {
                }
            });
            doc.add(table);
        }
    }

    private void addSessionHistory(Document doc, List<InterviewSession> sessions)
            throws DocumentException {
        Paragraph heading = new Paragraph("Recent Sessions (Last 10)", HEADING_FONT);
        heading.setSpacingAfter(10);
        doc.add(heading);

        PdfPTable table = new PdfPTable(new float[] { 3, 2, 2, 2, 2 });
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        // Header row
        for (String h : new String[] { "Role", "Type", "Difficulty", "Score", "Status" }) {
            PdfPCell cell = new PdfPCell(new Phrase(h, LABEL_FONT));
            cell.setBackgroundColor(new BaseColor(214, 234, 248));
            cell.setPadding(6);
            table.addCell(cell);
        }

        sessions.stream().limit(10).forEach(s -> {
            table.addCell(cell(s.getRole()));
            table.addCell(cell(s.getType()));
            table.addCell(cell(s.getDifficulty()));
            table.addCell(cell(String.valueOf(s.getScore() == null ? 0 : s.getScore())));
            table.addCell(cell(s.getStatus()));
        });

        doc.add(table);
    }

    private void addFooter(Document doc) throws DocumentException {
        LineSeparator line = new LineSeparator();
        doc.add(new Chunk(line));
        Paragraph footer = new Paragraph(
                "\nAI Interview Prep Platform  •  Keep practising, keep growing! 🚀",
                BODY_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Table Helpers ─────────────────────────────────────────────

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new BaseColor(248, 249, 250));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY_FONT));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private PdfPCell cell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", BODY_FONT));
        c.setPadding(5);
        return c;
    }
}
