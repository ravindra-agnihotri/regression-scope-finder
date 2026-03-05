package engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AIAnalysisService {

    private final String apiKey;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public AIAnalysisService(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public AIAnalysisResult analyzeMethod(String methodCode) {

        try {

            String prompt = buildPrompt(methodCode);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-5-nano");
            body.put("input", prompt);

            String requestBody = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String aiText = extractTextResponse(response.body());

            return mapper.readValue(aiText, AIAnalysisResult.class);

        } catch (Exception e) {
            throw new RuntimeException("AI call failed", e);
        }
    }

    private String extractTextResponse(String jsonResponse) throws Exception {

        JsonNode root = mapper.readTree(jsonResponse);

        // Check real error
        if (root.has("error") && !root.get("error").isNull()) {
            throw new RuntimeException("OpenAI API Error: "
                    + root.get("error").toPrettyString());
        }

        JsonNode outputArray = root.path("output");

        for (JsonNode node : outputArray) {

            if ("message".equals(node.path("type").asText())) {

                JsonNode contentArray = node.path("content");

                for (JsonNode content : contentArray) {

                    if ("output_text"
                            .equals(content.path("type").asText())) {

                        return content.path("text").asText();
                    }
                }
            }
        }

        throw new RuntimeException("No text output found in API response.");
    }

    private String buildPrompt(String methodCode) {

        return """
You are a senior Java automation architect.

Analyze this Java method.

Return strictly in JSON format:

{
  "summary": "...",
  "risk": "LOW | MEDIUM | HIGH",
  "reason": "...",
  "test_focus": ["...", "..."]
}

Method:
""" + methodCode;
    }
    public String analyzeMethodsBatch(String methodsCode){

        String prompt = """
You are a senior Java automation architect.

Analyze the following impacted methods.

For each method provide:
- summary
- risk (LOW/MEDIUM/HIGH)
- suggested regression tests.

Methods:

%s
""".formatted(methodsCode);

        return callOpenAI(prompt);

    }
    private String callOpenAI(String prompt) {

        try {

            String requestBody = """
        {
          "model": "gpt-5-nano",
          "input": "%s"
        }
        """.formatted(
                    prompt.replace("\"","\\\"")
                            .replace("\n","\\n")
            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.openai.com/v1/responses"))
                            .header("Authorization","Bearer " + apiKey)
                            .header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request,HttpResponse.BodyHandlers.ofString());

            return extractTextResponse(response.body());

        } catch (Exception e) {

            throw new RuntimeException("OpenAI call failed", e);

        }
    }
}