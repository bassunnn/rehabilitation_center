package ru.mirea.project.util;

import ru.mirea.project.exception.BusinessException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() {
    }

    public static void write(String fileName, List<String[]> rows) {
        Path path = Path.of(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        writer.write(';');
                    }
                    writer.write(escape(row[i]));
                }
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new BusinessException("Не удалось экспортировать данные в файл " + fileName, ex);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
