package com.solar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class DefectDetectionService {

    private final RestTemplate restTemplate;

    @Value("${ai.defect-service.url:http://localhost:8082/predict}")
    private String defectServiceUrl;

    public DefectDetectionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record DefectResult(String status, double confidence) {}

    public DefectResult detect(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                return null;
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<DefectResult> response = restTemplate.postForEntity(
                    defectServiceUrl, requestEntity, DefectResult.class);

            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
