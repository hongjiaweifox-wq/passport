package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

@Component
public class CsvFormulaNeutralizer {
    public byte[] render(List<ImportRowErrorEntity> errors) {
        try (var out = new java.io.StringWriter(); var printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader("row_no","source_record_id","field_code","error_code","safe_message").build())) {
            for (var error : errors) printer.printRecord(error.getRowNo(), neutralize(error.getSourceRecordId()), neutralize(error.getFieldCode()), neutralize(error.getErrorCode()), neutralize(error.getSafeMessage()));
            printer.flush(); return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Unable to render error report", ex); }
    }
    static String neutralize(String value) { if(value==null)return ""; if(!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0))>=0)return "'"+value; return value; }
}
