package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.exception.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Uploads owner business documents to Supabase Storage via its REST API.
 * Files are stored under owner-{userId}/ in the configured bucket; the returned
 * object path is persisted on {@code BusinessProfile} for later (signed) retrieval.
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket:business-documents}")
    private String bucket;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Uploads a document and returns its Supabase Storage object path
     * (e.g. {@code owner-12/registration_doc_1718300813.pdf}).
     *
     * @param docType file-name prefix, e.g. "registration_doc", "electricity_doc", "bank_doc"
     */
    public String upload(Long userId, String docType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new APIException("Document '" + docType + "' is required");
        }
        String objectPath = "owner-" + userId + "/" + docType + "_"
                + System.currentTimeMillis() + extension(file.getOriginalFilename());

        // Dev convenience: with no Supabase key configured, skip the real upload and
        // return the would-be object path so local registration testing still works.
        if (serviceKey == null || serviceKey.isBlank()) {
            log.warn("supabase.service-key is blank — skipping real upload for {} (dev mode)", objectPath);
            return objectPath;
        }

        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath;
        try {
            String contentType = file.getContentType() != null
                    ? file.getContentType() : "application/octet-stream";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new APIException("Document upload failed (" + response.statusCode()
                        + "): " + response.body());
            }
            return objectPath;
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            throw new APIException("Document upload failed: " + e.getMessage());
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
