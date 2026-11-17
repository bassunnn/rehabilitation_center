package ru.mirea.rehab.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.rehab.dto.ImportDtos.ImportMode;
import ru.mirea.rehab.dto.ImportDtos.ImportPreview;
import ru.mirea.rehab.dto.ImportDtos.ImportResult;
import ru.mirea.rehab.dto.PageResponse;
import ru.mirea.rehab.dto.PatientDtos.BulkRequest;
import ru.mirea.rehab.dto.PatientDtos.BulkResult;
import ru.mirea.rehab.dto.PatientDtos.PatientRequest;
import ru.mirea.rehab.dto.PatientDtos.PatientResponse;
import ru.mirea.rehab.service.CsvPatientService;
import ru.mirea.rehab.service.PatientService;
import ru.mirea.rehab.web.PageableFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/** REST-контур управления пациентами реабилитационного центра. */
@RestController
@RequestMapping({"/api/patients", "/api/items"})
@SecurityRequirement(name = "bearerAuth")
public class PatientController {
    private final PatientService patients;
    private final CsvPatientService csv;
    private final PageableFactory pageableFactory;

    public PatientController(PatientService patients, CsvPatientService csv, PageableFactory pageableFactory) {
        this.patients = patients;
        this.csv = csv;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','DOCTOR','ADMIN')")
    public PageResponse<PatientResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return PageResponse.from(patients.search(q, pageableFactory.patients(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','DOCTOR','ADMIN')")
    public PatientResponse get(@PathVariable UUID id) {
        return patients.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(201).body(patients.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return patients.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PatientResponse archive(@PathVariable UUID id) {
        return patients.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PatientResponse restore(@PathVariable UUID id) {
        return patients.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Confirm-Delete", required = false) String confirmation) {
        patients.permanentDelete(id, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return patients.bulk(request);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ImportPreview previewImport(@RequestPart("file") MultipartFile file,
                                       @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ImportResult applyImport(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public void export(@RequestParam(defaultValue = "") String q, HttpServletResponse response) throws IOException {
        String filename = "patients-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        csv.export(q, response.getOutputStream());
    }
}

