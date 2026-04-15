package com.main.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public interface ExcelService {



    byte[] generateExcelFromJson(Map<String, Object> jsonResponse) throws IOException;

    List<String> extractDocumentNumbers(MultipartFile file) throws IOException;

    boolean isExcelEmpty(byte[] excelData) throws IOException;

    byte[] createZipFromPdfFiles(List<byte[]> pdfFiles, List<String> fileNames);

    byte[] createZipFromPNGs(Map<String, List<byte[]>> files) throws IOException;
}
