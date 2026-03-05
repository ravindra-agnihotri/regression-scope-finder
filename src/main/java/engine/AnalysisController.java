package engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/analyze")
public class AnalysisController {

    private final RegressionAnalysisService service;
    private final GitDiffService diffService;
    private final GitCloneService cloneService;

    public AnalysisController() {

        AIAnalysisService ai =
                new AIAnalysisService("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");

        this.service = new RegressionAnalysisService(ai);
        this.diffService = new GitDiffService();
        this.cloneService = new GitCloneService();
    }

    @GetMapping("/stream")
    public SseEmitter analyzeStream(
            @RequestParam String repoUrl,
            @RequestParam String baseBranch,
            @RequestParam String targetBranch) {

        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {

            try {

                ObjectMapper mapper = new ObjectMapper();

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("Cloning repository..."));

                String cloneDir = "./workspace/repo";

                cloneService.cloneRepository(repoUrl, cloneDir);

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("Detecting changed files..."));

                List<String> changedFiles =
                        diffService.getChangedJavaFiles(
                                cloneDir,
                                baseBranch,
                                targetBranch
                        );

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("Analyzing methods..."));
                //added
                CallGraphBuilder graphBuilder =
                        new CallGraphBuilder();

                Map<String, Set<String>> graph =
                        graphBuilder.buildGraph("./workspace/repo");

                emitter.send(SseEmitter.event()
                        .name("graph")
                        .data(mapper.writeValueAsString(graph)));
                //till here

                List<RegressionAnalysisService.MethodRisk> result =
                        service.analyze(repoUrl, baseBranch, targetBranch);

                AnalysisResponse response = new AnalysisResponse();

                response.changedFiles = changedFiles;

                response.methods = result;

                response.riskSummary = Map.of(
                        "HIGH", result.stream().filter(r -> r.result.getRisk().equals("HIGH")).count(),
                        "MEDIUM", result.stream().filter(r -> r.result.getRisk().equals("MEDIUM")).count(),
                        "LOW", result.stream().filter(r -> r.result.getRisk().equals("LOW")).count()
                );

                response.suggestions = List.of(
                        "Login Tests",
                        "Authentication Flow",
                        "Password Validation"
                );

                emitter.send(SseEmitter.event()
                        .name("result")
                        .data(mapper.writeValueAsString(response)));

                emitter.complete();

            } catch (Exception e) {

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> errorPayload = new LinkedHashMap<>();
                    errorPayload.put("message", e.getMessage() == null ? "Analysis failed" : e.getMessage());

                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(mapper.writeValueAsString(errorPayload)));
                } catch (Exception ignored) {
                    // Ignore secondary emitter errors and complete the stream gracefully.
                }

                emitter.complete();

            }

        }).start();

        return emitter;
    }


}