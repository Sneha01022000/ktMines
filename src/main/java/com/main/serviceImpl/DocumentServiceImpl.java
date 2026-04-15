package com.main.serviceImpl;

import com.main.service.DocumentService;
import com.main.service.ktMINESService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.main.util.ConstKeyStrings.DOCUMENT_NUMBER;

@Component
public class DocumentServiceImpl implements DocumentService {


    @Autowired
    ktMINESService ktMINESService;
    @Override
    public void processDocuments(List<Map<String, Object>> items, List<String> documentNumbers,
                                 Map<String, List<byte[]>> successfulFiles, Map<String, String> fetchErrors, String apiKey) {
        List<CompletableFuture<Void>> futures = items.stream()
                .filter(item -> documentNumbers.contains(item.get(DOCUMENT_NUMBER)))
                .map(item -> CompletableFuture.runAsync(() -> {
                    String docNumber = (String) item.get(DOCUMENT_NUMBER);
                    List<String> imagePaths = (List<String>) item.get("imagePaths");

                    if (imagePaths == null || imagePaths.isEmpty()) {
                        fetchErrors.put(docNumber, "No image paths found");
                        return;
                    }

                    List<byte[]> images = fetchImages(docNumber, apiKey, imagePaths.stream().toList());
                    if (!images.isEmpty()) successfulFiles.put(docNumber, images);
                    else fetchErrors.put(docNumber, "Failed to fetch images");
                })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    private List<byte[]> fetchImages(String documentNumber, String apiKey, List<String> imagePaths) {
        return imagePaths.parallelStream()
                .map(imagePath -> ktMINESService.fetchPatentPng(documentNumber, apiKey, imagePath))
                .filter(responseEntity -> responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null)
                .map(ResponseEntity::getBody)
                .collect(Collectors.toList());
    }

}

