package com.example.financemgmt.controller;

import com.example.financemgmt.exception.BadRequestException;
import com.example.financemgmt.service.DatabaseBackupService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/api/backup")
public class DatabaseBackupController {
    private final DatabaseBackupService service;

    public DatabaseBackupController(DatabaseBackupService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<byte[]> export() {
        String filename = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH)) + ".sql";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/sql"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(service.exportSql());
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse importBackup(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BadRequestException("Choose a non-empty backup file");
        if (file.getSize() > 50L * 1024 * 1024) throw new BadRequestException("Backup files are limited to 50 MB");
        service.importSql(file.getBytes());
        return new ImportResponse("Database restored. ID sequences were reset for new records.");
    }

    public record ImportResponse(String message) {}
}
