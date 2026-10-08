package com.solar.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.solar.backend.model.Currency;
import com.solar.backend.model.Farm;
import com.solar.backend.model.dto.DashboardStatsDTO;
import com.solar.backend.model.dto.InspectionDTO;
import com.solar.backend.model.dto.PanelGridDTO;
import com.solar.backend.repository.FarmRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    
    
    private static final Color DEFAULT_ACCENT = new Color(17, 28, 45);

    private final InspectionService inspectionService;
    private final PanelService panelService;
    private final FarmRepository farmRepository;

    public ReportService(InspectionService inspectionService, PanelService panelService, FarmRepository farmRepository) {
        this.inspectionService = inspectionService;
        this.panelService = panelService;
        this.farmRepository = farmRepository;
    }

    public byte[] generateExcelReport(Long farmId) throws IOException {
        Farm farm = resolveFarm(farmId);
        List<InspectionDTO> inspections = inspectionService.getAllReports(farmId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inspection History");
            Color accent = accentColor(farm);

            
            
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(companyName(farm) + " - Inspection History");
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (headerStyle instanceof XSSFCellStyle xssfHeaderStyle) {
                xssfHeaderStyle.setFillForegroundColor(new XSSFColor(accent, null));
            } else {
                headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            }

            String[] columns = {"ID", "Date", "Panel", "Inspection Type", "Status", "Defect Type", "Confidence", "Estimated Cost", "Currency"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (InspectionDTO inspection : inspections) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inspection.getId());
                row.createCell(1).setCellValue(inspection.getInspectionDate() != null
                        ? inspection.getInspectionDate().format(DateTimeFormatter.ISO_DATE) : "");
                row.createCell(2).setCellValue(inspection.getPanelRow() != null
                        ? inspection.getPanelRow() + "-" + inspection.getPanelColumn() : "N/A");
                row.createCell(3).setCellValue(inspection.getInspectionType() != null ? inspection.getInspectionType() : "RGB");
                row.createCell(4).setCellValue(inspection.getStatus() != null ? inspection.getStatus() : inspection.getProcessingStatus());
                row.createCell(5).setCellValue(inspection.getDefectType() != null ? inspection.getDefectType() : "-");
                row.createCell(6).setCellValue(inspection.getConfidenceScore() != null ? inspection.getConfidenceScore() : 0.0);
                row.createCell(7).setCellValue(inspection.getEstimatedRepairCost() != null ? inspection.getEstimatedRepairCost() : 0.0);
                row.createCell(8).setCellValue(inspection.getCostCurrency() != null ? inspection.getCostCurrency() : "USD");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generatePdfReport(Long farmId) throws DocumentException, IOException {
        Farm farm = resolveFarm(farmId);
        DashboardStatsDTO stats = inspectionService.getDashboardStats(farmId);
        List<PanelGridDTO> panels = panelService.getPanelGrid(farmId);
        List<InspectionDTO> inspections = inspectionService.getAllReports(farmId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        Color accent = accentColor(farm);
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, accent);
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        if (farm != null && farm.getReportLogoPath() != null) {
            try {
                Image logo = Image.getInstance(farm.getReportLogoPath());
                logo.scaleToFit(120, 60);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(new Paragraph(" "));
            } catch (Exception e) {
                
                
                System.err.println("Failed to embed farm logo in PDF report: " + e.getMessage());
            }
        }

        Paragraph title = new Paragraph(companyName(farm) + " - Farm Health Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph generatedOn = new Paragraph(
                "Generated on " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                normalFont);
        generatedOn.setAlignment(Element.ALIGN_CENTER);
        generatedOn.setSpacingAfter(20);
        document.add(generatedOn);

        document.add(new Paragraph("Summary", sectionFont));
        document.add(new Paragraph(" "));

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        addSummaryRow(summaryTable, "Total Inspections", String.valueOf(stats.getTotalInspections()));
        addSummaryRow(summaryTable, "Defects Found", String.valueOf(stats.getDefectsFound()));
        addSummaryRow(summaryTable, "Healthy Panels", String.valueOf(stats.getHealthyPanels()));
        Currency reportCurrency = Currency.fromString(stats.getCurrency());
        addSummaryRow(summaryTable, "Total Estimated Repair Cost",
                formatCost(stats.getTotalEstimatedRepairCost(), reportCurrency));
        document.add(summaryTable);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Repair Cost by Defect Type", sectionFont));
        document.add(new Paragraph(" "));
        addCostBreakdownChart(document, inspections, accent, reportCurrency, normalFont);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Panel Status Breakdown", sectionFont));
        document.add(new Paragraph(" "));

        PdfPTable panelTable = new PdfPTable(5);
        panelTable.setWidthPercentage(100);
        addHeaderCell(panelTable, "Panel", accent);
        addHeaderCell(panelTable, "Status", accent);
        addHeaderCell(panelTable, "Defect Type", accent);
        addHeaderCell(panelTable, "Last Inspection", accent);
        addHeaderCell(panelTable, "Est. Cost", accent);

        for (PanelGridDTO panel : panels) {
            panelTable.addCell(new Phrase(panel.getRowNumber() + "-" + panel.getColumnNumber(), normalFont));
            panelTable.addCell(new Phrase(panel.getLatestStatus() != null ? panel.getLatestStatus() : "NO_DATA", normalFont));
            panelTable.addCell(new Phrase(panel.getLatestDefectType() != null ? panel.getLatestDefectType() : "-", normalFont));
            panelTable.addCell(new Phrase(panel.getLastInspectionDate() != null ? panel.getLastInspectionDate().toString() : "-", normalFont));
            String costText = panel.getLatestEstimatedRepairCost() != null
                    ? formatCost(panel.getLatestEstimatedRepairCost(), Currency.fromString(panel.getLatestCostCurrency()))
                    : "-";
            panelTable.addCell(new Phrase(costText, normalFont));
        }
        document.add(panelTable);

        document.close();
        return out.toByteArray();
    }

    
    
    
    
    
    
    private void addCostBreakdownChart(Document document, List<InspectionDTO> inspections, Color accent,
                                        Currency currency, Font labelFont) throws DocumentException {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (InspectionDTO inspection : inspections) {
            String defectType = inspection.getDefectType();
            if (defectType == null || defectType.equals("NONE")) continue;
            double cost = inspection.getEstimatedRepairCost() != null ? inspection.getEstimatedRepairCost() : 0.0;
            totals.merge(defectType, cost, Double::sum);
        }

        if (totals.isEmpty()) {
            document.add(new Paragraph("No repair costs recorded yet.", labelFont));
            return;
        }

        double max = totals.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max <= 0) max = 1.0;
        final double maxValue = max;

        List<Map.Entry<String, Double>> sorted = totals.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed())
                .toList();

        PdfPTable chart = new PdfPTable(new float[]{22, 58, 20});
        chart.setWidthPercentage(100);
        chart.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        for (Map.Entry<String, Double> entry : sorted) {
            String label = prettifyDefectType(entry.getKey());
            double value = entry.getValue();
            
            
            
            float filledPct = (float) Math.min(98.0, Math.max(2.0, (value / maxValue) * 100.0));

            PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            labelCell.setPaddingBottom(6);
            chart.addCell(labelCell);

            PdfPTable bar = new PdfPTable(new float[]{filledPct, 100 - filledPct});
            bar.setWidthPercentage(100);
            PdfPCell filled = new PdfPCell();
            filled.setBackgroundColor(accent);
            filled.setFixedHeight(10);
            filled.setBorder(Rectangle.NO_BORDER);
            bar.addCell(filled);
            PdfPCell empty = new PdfPCell();
            empty.setBackgroundColor(new Color(230, 233, 238));
            empty.setFixedHeight(10);
            empty.setBorder(Rectangle.NO_BORDER);
            bar.addCell(empty);

            PdfPCell barCell = new PdfPCell(bar);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            barCell.setPaddingBottom(6);
            chart.addCell(barCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(formatCost(value, currency), labelFont));
            valueCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valueCell.setPaddingBottom(6);
            chart.addCell(valueCell);
        }

        document.add(chart);
    }

    private String prettifyDefectType(String defectType) {
        String[] words = defectType.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private Farm resolveFarm(Long farmId) {
        if (farmId == null) return null;
        return farmRepository.findById(farmId).orElse(null);
    }

    private String companyName(Farm farm) {
        if (farm != null && farm.getReportCompanyName() != null && !farm.getReportCompanyName().isBlank()) {
            return farm.getReportCompanyName();
        }
        if (farm != null) return farm.getName();
        return "Axela";
    }

    private Color accentColor(Farm farm) {
        if (farm == null || farm.getReportAccentColor() == null) return DEFAULT_ACCENT;
        try {
            return Color.decode(farm.getReportAccentColor());
        } catch (NumberFormatException e) {
            return DEFAULT_ACCENT;
        }
    }

    private String formatCost(double amount, Currency currency) {
        
        
        return currency == Currency.RON
                ? String.format("%.2f %s", amount, currency.getSymbol())
                : currency.getSymbol() + String.format("%.2f", amount);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Color background) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
