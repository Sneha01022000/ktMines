package com.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.service.DocumentService;
import com.main.service.ExcelService;
import com.main.service.ktMINESService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.main.util.ConstKeyStrings.*;


@RestController
@RequestMapping("/api/v1/excel")
public class ExcelController {

    @Autowired
    ExcelService excelService;

    @Autowired
    ktMINESService ktMINESService;

    @Autowired
    DocumentService documentService;

    @PostMapping(value = "/importExcel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> processExcel(@RequestParam("file") MultipartFile file, @RequestParam String apiKey) {
        try {
            List<String> documentNumbers = excelService.extractDocumentNumbers(file);

            Map<String, Object> apiResponse = ktMINESService.callBulkApi(documentNumbers, apiKey);

            List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map<String, Object>) apiResponse.get(RESPONSE)).get("items");

            if (items == null || items.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR, "There is no matched records found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            byte[] excelData = excelService.generateExcelFromJson(apiResponse);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exported_data.xlsx").contentType(MediaType.APPLICATION_OCTET_STREAM).body(excelData);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR, "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/search-patents")
    public ResponseEntity<Object> searchPatents(@RequestParam(required = false) String q,
                                                @RequestParam String apiKey,
                                                @RequestParam(defaultValue = "0") int start,
                                                @RequestParam(defaultValue = "10") int count) {
        try {
            List<String> documentNumbers = ktMINESService.callSearchApi(q, apiKey, start, count);
            Map<String, Object> apiResponse = ktMINESService.callBulkApi(documentNumbers, apiKey);
            List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map<String, Object>) apiResponse.get(RESPONSE)).get("items");

            if (items == null || items.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put(ERROR, "There is no matched records found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            byte[] excelData = excelService.generateExcelFromJson(apiResponse);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exported_data.xlsx").contentType(MediaType.APPLICATION_OCTET_STREAM).body(excelData);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(ERROR, "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/downloadPatentPDF", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> getPatentPdf(@RequestParam("file") MultipartFile file,
                                               @RequestParam(name = "apiKey", required = true) String apiKey) throws IOException {
        List<String> documentNumbers = excelService.extractDocumentNumbers(file);
        Map<String, byte[]> successfulFiles = new ConcurrentHashMap<>();
        List<String> failedDocuments = Collections.synchronizedList(new ArrayList<>());

        documentNumbers.parallelStream().forEach(docdbDocumentNumber -> {
            ResponseEntity<byte[]> response = ktMINESService.fetchPatentPdf(docdbDocumentNumber, apiKey);
            if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                failedDocuments.add(docdbDocumentNumber);
            } else {
                successfulFiles.put(docdbDocumentNumber, response.getBody());
            }
        });

        if (successfulFiles.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR, "None of the document numbers have PDFs ");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ObjectMapper().writeValueAsBytes(errorResponse));
        }

        byte[] zipData = excelService.createZipFromPdfFiles(new ArrayList<>(successfulFiles.values()),
                new ArrayList<>(successfulFiles.keySet()));

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        responseHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("PATENTS_PDF.zip").build());

        if (!failedDocuments.isEmpty()) {
            responseHeaders.add("X-Failed-Documents", String.join(",", failedDocuments));
        }

        return ResponseEntity.ok().headers(responseHeaders).body(zipData);
    }

    @PostMapping(value = "/imageDownload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> processPatentImages(@RequestParam("file") MultipartFile file,
                                           @RequestParam(name = "apiKey", required = true) String apiKey) throws IOException {
        List<String> documentNumbers = excelService.extractDocumentNumbers(file);
        Map<String, Object> apiResponse = ktMINESService.callBulkApi(documentNumbers, apiKey);

        if (apiResponse == null || !apiResponse.containsKey(RESPONSE)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR, "Invalid API response"));
        }

        Map<String, Object> response = (Map<String, Object>) apiResponse.get(RESPONSE);
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        Map<String, List<byte[]>> successfulFiles = new HashMap<>();
        Map<String, String> fetchErrors = new HashMap<>();

        documentService.processDocuments(items, documentNumbers, successfulFiles, fetchErrors, apiKey);

        if (!successfulFiles.isEmpty()) {
            byte[] zipData = excelService.createZipFromPNGs(successfulFiles);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename("PATENTS_Images.zip").build());
            return ResponseEntity.ok().headers(headers).body(zipData);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR, "None of the documents has images"));
    }



}

