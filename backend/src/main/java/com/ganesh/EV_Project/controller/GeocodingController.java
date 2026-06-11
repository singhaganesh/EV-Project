package com.ganesh.EV_Project.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Server-side proxy for Nominatim (OSM) geocoding. The browser cannot set a
 * User-Agent (a forbidden header), and Nominatim's usage policy requires a
 * descriptive one, so geocoding is proxied here with the required header and a
 * timeout. Returns Nominatim's JSON unchanged.
 */
@RestController
@RequestMapping("/api/geocode")
public class GeocodingController {

    @Value("${app.nominatim.user-agent:PlugsyEV/1.0 (support@plugsy.in)}")
    private String userAgent;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String q) {
        String url = "https://nominatim.openstreetmap.org/search?q="
                + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&format=json&countrycodes=in&limit=5&addressdetails=1";
        return proxy(url);
    }

    @GetMapping("/reverse")
    public ResponseEntity<String> reverse(@RequestParam double lat, @RequestParam double lon) {
        String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lon
                + "&format=json&addressdetails=1";
        return proxy(url);
    }

    private ResponseEntity<String> proxy(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", "en")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(response.statusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.body());
        } catch (Exception e) {
            // Fall back to an empty result rather than failing the caller
            return ResponseEntity.status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("[]");
        }
    }
}
