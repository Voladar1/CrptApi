package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String postUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore semaphore;

    public static void main(String[] args) {
        var crptApi = new CrptApi(TimeUnit.MINUTES,4);
        var objectTest = "{\"description\":{\"participantInn\":\"string\"},\"doc_id\":\"string\",\"doc_status\":\"string\",\"doc_type\":\"LP_INTRODUCE_GOODS\",\"importRequest\":true,\"owner_inn\":\"string\",\"participant_inn\":\"string\",\"producer_inn\":\"string\",\"production_date\":\"2020-01-23\",\"production_type\":\"string\",\"products\":[{\"certificate_document\":\"string\",\"certificate_document_date\":\"2020-01-23\",\"certificate_document_number\":\"string\",\"owner_inn\":\"string\",\"producer_inn\":\"string\",\"production_date\":\"2020-01-23\",\"tnved_code\":\"string\",\"uit_code\":\"string\",\"uitu_code\":\"string\"}],\"reg_date\":\"2020-01-23\",\"reg_number\":\"string\"}";

        Runnable task = () -> crptApi.sendObject(objectTest, "Signature");

        for (int i = 0; i < 10; i++) {
            new Thread(task).start();
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit);

            System.out.println("request count refreshed.");
        }, 1, 1, timeUnit);
    }

    public void sendObject(Object document, String signature) {
        try {
            System.out.println("Sending queued");
            semaphore.acquire();
            System.out.println("Sending started");
            // Populate your Document object here
            //Document document = new Document();

            var body = createRequestBody(document);

            if (body == null) {
                return;
            }

            System.out.println("Body created");
            postDocument(body);

            System.out.println("Document created with signature: " + signature);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    private String createRequestBody(Object document) {
        ObjectMapper objectMapper = new ObjectMapper();
        // to serialize object
        //String json = objectMapper.writeValueAsString(document);
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void postDocument(String body) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(postUrl);
            postRequest.setEntity(new StringEntity(body));
            postRequest.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            System.out.println("Unexpected error on httpClient: " + e.getMessage());
        }
    }
}
