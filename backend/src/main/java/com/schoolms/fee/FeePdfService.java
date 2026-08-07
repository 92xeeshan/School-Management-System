package com.schoolms.fee;

import com.schoolms.file.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders fee payment receipts to PDF using a compiled JasperReports template
 * and stores them in MinIO. The object key is tenant-scoped like all uploads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeePdfService {

    private final MinioService minioService;

    public String generateReceipt(UUID schoolId, Map<String, Object> fields) {
        try {
            ClassPathResource resource = new ClassPathResource("reports/fee_receipt.jrxml");
            try (InputStream in = resource.getInputStream()) {
                JasperReport report = JasperCompileManager.compileReport(in);
                Map<String, Object> params = new HashMap<>(fields);
                JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
                byte[] pdf = JasperExportManager.exportReportToPdf(print);
                String objectKey = schoolId + "/receipts/" + UUID.randomUUID() + ".pdf";
                minioService.uploadBytes(schoolId, "receipts", objectKey, pdf, "application/pdf");
                return minioService.presignedUrl(objectKey);
            }
        } catch (Exception e) {
            log.error("Failed to generate fee receipt PDF", e);
            return null;
        }
    }
}
