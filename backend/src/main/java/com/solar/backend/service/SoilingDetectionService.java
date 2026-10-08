package com.solar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@Service
public class SoilingDetectionService {

    private final RestTemplate restTemplate;

    @Value("${ai.soiling-service.url:http://localhost:8081/predict}")
    private String soilingServiceUrl;

    public SoilingDetectionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record SoilingResult(String status, double confidence) {
    }

    public SoilingResult detect(String imagePath) {
        if (imagePath == null) return null;
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) return null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(imageFile));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(soilingServiceUrl, request, Map.class);
            if (response == null) return null;

            String status = String.valueOf(response.get("status"));
            double confidence = response.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
            return new SoilingResult(status, confidence);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Soiling detection service unreachable, falling back to simulation: " + e.getMessage());
            return null;
        }
    }
}