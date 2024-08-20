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
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private static final String postUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore semaphore;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public static void main(String[] args) {
        var crptApi = new CrptApi(TimeUnit.MINUTES,4);
        var objectTest = "{\"description\":{\"participantInn\":\"string\"},\"doc_id\":\"string\",\"doc_status\":\"string\",\"doc_type\":\"LP_INTRODUCE_GOODS\",\"importRequest\":true,\"owner_inn\":\"string\",\"participant_inn\":\"string\",\"producer_inn\":\"string\",\"production_date\":\"2020-01-23\",\"production_type\":\"string\",\"products\":[{\"certificate_document\":\"string\",\"certificate_document_date\":\"2020-01-23\",\"certificate_document_number\":\"string\",\"owner_inn\":\"string\",\"producer_inn\":\"string\",\"production_date\":\"2020-01-23\",\"tnved_code\":\"string\",\"uit_code\":\"string\",\"uitu_code\":\"string\"}],\"reg_date\":\"2020-01-23\",\"reg_number\":\"string\"}";
        var document = new Document();
        document.doc_id = "133";
        document.doc_status = "125";
        document.doc_type = "123";
        document.importRequest = true;
        document.owner_inn = "123";

        var product = new Product();
        product.production_date = "2024-04-31";
        product.certificate_document = "license";
        product.owner_inn = "Boss";
        product.producer_inn = "Valera";
        document.products = new ArrayList<Product>();
        document.products.add(product);

        var description = new Description();
        description.participantInn = "ring";
        document.description = description;

        Runnable task = () -> crptApi.sendObject(document, "Signature");
        Runnable task2 = () -> {
            try {
                Thread.sleep(61000);
                System.out.println("Task2");
                crptApi.sendObject(objectTest, "Signature");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Task was interrupted: " + e.getMessage());
            }
        };

        for (int i = 0; i < 3; i++) {
            new Thread(task).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread(task2).start();
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestCount.get());
            requestCount.set(0);
            System.out.println("request count refreshed.");
        }, 1, 1, timeUnit);
    }

    public void sendObject(Object document, String signature) {
        try {
            System.out.println("Sending queued");
            semaphore.acquire();
            System.out.println("Sending started");

            requestCount.incrementAndGet();

            var body = createRequestBody(document);

            if (body == null) {
                return;
            }

            System.out.println("Body created: " + body);
            postDocument(body);

            System.out.println("Document created with signature: " + signature);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    private String createRequestBody(Object document) {
        ObjectMapper objectMapper = new ObjectMapper();
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

    public static class Description{
        public String participantInn;
    }

    public static class Product{
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
    }

    public static class Document{
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public ArrayList<Product> products;
        public String reg_date;
        public String reg_number;
    }
}
