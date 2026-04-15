package com.main.serviceImpl;

import com.main.service.ktMINESService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static com.main.util.ConstKeyStrings.DOCUMENT_NUMBER;
import static com.main.util.ConstKeyStrings.RESPONSE;

@Component
public class ktMINESServiceImpl implements ktMINESService {

    @Value("${bulkApiUrl}")
    private String bulkApiUrl;

    @Value("${searchApiUrl}")
    private String searchApiUrl;

    @Value("${pdfApiUrl}")
    private String pdfApiUrl;

    @Value("${pngApiUrl}")
    private String pngApiUrl;

    @Override
    public Map<String, Object> callBulkApi(List<String> documentNumbers, String apiKey) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("documentNumbers", documentNumbers);
        requestBody.put("key", apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                bulkApiUrl, HttpMethod.POST, requestEntity, Map.class
        );

        return responseEntity.getBody();
    }

    @Override
    public List<String> callSearchApi(String q, String apiKey, int start, int count) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = Map.of(
                "q", q,
                "key", apiKey,
                "start", start,
                "count", count
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    searchApiUrl, HttpMethod.POST, requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.ofNullable(response.getBody())
                    .map(body -> (Map<String, Object>) body.get(RESPONSE))
                    .map(responseData -> (List<Map<String, Object>>) responseData.get("items"))
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(item -> item.get(DOCUMENT_NUMBER))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }


    @Override
    public ResponseEntity<byte[]> fetchPatentPdf(String docdbDocumentNumber, String apiKey) {
        RestTemplate restTemplate = new RestTemplate();

        String url = String.format(pdfApiUrl, docdbDocumentNumber, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("application/pdf"));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return ResponseEntity.status(response.getStatusCode()).body(null);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename(docdbDocumentNumber + ".pdf").build());

            return ResponseEntity.ok().headers(responseHeaders).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Override
    public ResponseEntity<byte[]> fetchPatentPng(String docdbDocumentNumber, String apiKey, String imagePath) {
        RestTemplate restTemplate = new RestTemplate();

        String url = String.format(pngApiUrl, docdbDocumentNumber, apiKey, "40%", imagePath );

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("application/pdf"));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return ResponseEntity.status(response.getStatusCode()).body(null);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename(docdbDocumentNumber + ".pdf").build());

            return ResponseEntity.ok().headers(responseHeaders).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}



