package com.main.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface DocumentService {



    void processDocuments(List<Map<String, Object>> items, List<String> documentNumbers,
                          Map<String, List<byte[]>> successfulFiles, Map<String, String> fetchErrors, String apiKey);
}
