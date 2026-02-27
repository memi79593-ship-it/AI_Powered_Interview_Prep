package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.service.PdfReportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Day 11 – PDF Report Download Controller.
 *
 * GET /api/report/{email}
 * Returns a downloadable PDF performance report for the given email.
 */
@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class ReportController {

    private final PdfReportService pdfReportService;

    public ReportController(PdfReportService pdfReportService) {
        this.pdfReportService = pdfReportService;
    }

    /**
     * GET /api/report/{email}
     * Returns a PDF file download.
     *
     * Example: GET http://localhost:8080/api/report/user@test.com
     * Browser or Postman will download: interview_report_user@test.com.pdf
     */
    @GetMapping("/{email}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String email) {
        byte[] pdf = pdfReportService.generateReport(email);

        String filename = "interview_report_" + email.replace("@", "_at_") + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
