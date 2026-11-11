package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.EntityType;
import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.tuya.passport.cloud.l2.support.ApiException;

@Component
public class CsvImportParser {
    @FunctionalInterface public interface RecordHandler { void accept(int rowNo, CSVRecord record); }
    public int parse(Path path, EntityType type, int maxRows, RecordHandler handler) {
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(Files.newInputStream(path), decoder))) {
            reader.mark(1);
            int first = reader.read();
            if (first != 0xFEFF && first != -1) reader.reset();
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(false).build();
            try (CSVParser parser = format.parse(reader)) {
                List<String> actual = parser.getHeaderNames().stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();
                String[] expected = type.headers();
                String[] localized = type.templateHeaders();
                boolean matches = actual.size() == expected.length || actual.size() == type.legacyHeaderLength();
                for (int i = 0; matches && i < actual.size(); i++) {
                    String header = actual.get(i);
                    matches = header.equals(expected[i].toLowerCase(Locale.ROOT))
                        || header.equals(localized[i].trim().toLowerCase(Locale.ROOT));
                }
                if (!matches) throw new ApiException("INGEST_SCHEMA_MISMATCH", "CSV 表头与所选模板不匹配，请使用模板表头（中英文均可）并保持字段顺序", HttpStatus.UNPROCESSABLE_ENTITY);
                int count = 0;
                for (CSVRecord record : parser) {
                    if (record.size() == 1 && record.get(0).isBlank()) continue;
                    count++;
                    if (count > maxRows) throw new ApiException("INGEST_ROW_LIMIT_EXCEEDED", "CSV 行数超过上限 " + maxRows, HttpStatus.UNPROCESSABLE_ENTITY);
                    handler.accept((int) record.getRecordNumber() + 1, record);
                }
                if (count == 0) throw new ApiException("INGEST_NO_DATA_ROWS", "CSV 文件只有表头，没有数据行", HttpStatus.UNPROCESSABLE_ENTITY);
                return count;
            }
        } catch (ApiException ex) { throw ex; }
        catch (IOException | IllegalArgumentException ex) { throw new ApiException("INGEST_PARSE_ERROR", "CSV 文件无法解析，请检查 UTF-8 编码和 CSV 格式", HttpStatus.UNPROCESSABLE_ENTITY); }
    }
}
