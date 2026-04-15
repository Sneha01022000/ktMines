package com.main.service;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public interface ktMINESService {

    Map<String, Object> callBulkApi(List<String> documentNumbers, String apiKey);


    List<String> callSearchApi(String q, String apiKey, int start, int count);

    ResponseEntity<byte[]> fetchPatentPdf(String docdbDocumentNumber, String apiKey);

    ResponseEntity<byte[]> fetchPatentPng(String docdbDocumentNumber, String apiKey, String imagePath);
}
