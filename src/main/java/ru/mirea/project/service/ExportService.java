package ru.mirea.project.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.project.model.Patient;
import ru.mirea.project.model.Procedure;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public void exportToExcel(File file, List<Patient> patients, List<Procedure> procedures) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Стили
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Лист 1: Пациенты
            if (patients != null) {
                Sheet sheetPatients = workbook.createSheet("Пациенты");
                String[] patientHeaders = {"ID", "Фамилия", "Имя", "Возраст", "Дата рождения", "Телефон", "Email"};
                Row headerRow = sheetPatients.createRow(0);
                for (int i = 0; i < patientHeaders.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(patientHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (Patient p : patients) {
                    Row row = sheetPatients.createRow(rowIdx++);
                    createCell(row, 0, p.getId() != null ? p.getId().toString() : "", dataStyle);
                    createCell(row, 1, p.getLastName(), dataStyle);
                    createCell(row, 2, p.getFirstName(), dataStyle);
                    createCell(row, 3, String.valueOf(p.getAge()), dataStyle);
                    createCell(row, 4, p.getFormattedBirthDate(), dataStyle);
                    createCell(row, 5, p.getPhone(), dataStyle);
                    createCell(row, 6, p.getEmail(), dataStyle);
                }

                for (int i = 0; i < patientHeaders.length; i++) {
                    sheetPatients.autoSizeColumn(i);
                }
            }

            // Лист 2: Процедуры
            if (procedures != null) {
                Sheet sheetProcedures = workbook.createSheet("Процедуры");
                String[] procHeaders = {"ID", "Пациент", "Название процедуры", "Дата", "Статус", "Описание"};
                Row headerRow = sheetProcedures.createRow(0);
                for (int i = 0; i < procHeaders.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(procHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (Procedure p : procedures) {
                    Row row = sheetProcedures.createRow(rowIdx++);
                    createCell(row, 0, p.getId() != null ? p.getId().toString() : "", dataStyle);
                    createCell(row, 1, p.getPatientFullName() != null ? p.getPatientFullName() : "ID: " + p.getPatientId(), dataStyle);
                    createCell(row, 2, p.getName(), dataStyle);
                    createCell(row, 3, p.getFormattedProcedureDate(), dataStyle);
                    createCell(row, 4, p.getStatusTitle(), dataStyle);
                    createCell(row, 5, p.getDescription() != null ? p.getDescription() : "", dataStyle);
                }

                for (int i = 0; i < procHeaders.length; i++) {
                    sheetProcedures.autoSizeColumn(i);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            logger.info("Данные успешно экспортированы в Excel: {}", file.getAbsolutePath());
        }
    }

    public void exportPatientsToCsv(File file, List<Patient> patients) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // UTF-8 BOM для корректного открытия в Excel на русском языке
            writer.print('\uFEFF');
            writer.println("ID;Фамилия;Имя;Возраст;Дата рождения;Телефон;Email");

            for (Patient p : patients) {
                writer.println(String.format("%s;%s;%s;%d;%s;%s;%s",
                        escapeCsv(p.getId() != null ? p.getId().toString() : ""),
                        escapeCsv(p.getLastName()),
                        escapeCsv(p.getFirstName()),
                        p.getAge(),
                        escapeCsv(p.getFormattedBirthDate()),
                        escapeCsv(p.getPhone()),
                        escapeCsv(p.getEmail())
                ));
            }
            logger.info("Пациенты успешно экспортированы в CSV: {}", file.getAbsolutePath());
        }
    }

    public void exportProceduresToCsv(File file, List<Procedure> procedures) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.print('\uFEFF');
            writer.println("ID;Пациент;Название процедуры;Дата;Статус;Описание");

            for (Procedure p : procedures) {
                writer.println(String.format("%s;%s;%s;%s;%s;%s",
                        escapeCsv(p.getId() != null ? p.getId().toString() : ""),
                        escapeCsv(p.getPatientFullName() != null ? p.getPatientFullName() : "ID: " + p.getPatientId()),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getFormattedProcedureDate()),
                        escapeCsv(p.getStatusTitle()),
                        escapeCsv(p.getDescription() != null ? p.getDescription() : "")
                ));
            }
            logger.info("Процедуры успешно экспортированы в CSV: {}", file.getAbsolutePath());
        }
    }

    private void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

