package engine;

import util.Utils;
import java.util.*;

public class RegressionAnalysisService {

    private final AIAnalysisService aiService;

    public RegressionAnalysisService(AIAnalysisService aiService) {
        this.aiService = aiService;
    }

    public List<MethodRisk> analyze(
            String repoUrl,
            String baseBranch,
            String targetBranch) {

        String cloneDir = "./workspace/repo";

        GitCloneService cloneService = new GitCloneService();
        System.out.println("Cloning repository...");
        cloneService.cloneRepository(repoUrl, cloneDir);

        GitDiffService diffService = new GitDiffService();
        System.out.println("Detecting changed files...");

        List<String> changedFiles =
                diffService.getChangedJavaFiles(
                        cloneDir, baseBranch, targetBranch);

        System.out.println("Changed Files: " + changedFiles);

        DiffLineExtractor diffExtractor = new DiffLineExtractor();
        MethodImpactAnalyzer analyzer = new MethodImpactAnalyzer();

        List<MethodRisk> results = new ArrayList<>();

        for (String file : changedFiles) {

            System.out.println(file + " is changed file");
            System.out.println("Analyzing methods...");

            Set<Integer> changedLines =
                    diffExtractor.extractChangedLines(
                            cloneDir, baseBranch, targetBranch, file);

            String fullPath = cloneDir + "/" + file;

            Set<String> impactedMethods =
                    analyzer.findImpactedMethods(fullPath, changedLines);

            for (String method : impactedMethods) {

                try {

                    String code = analyzer.extractFullMethodCode(
                            fullPath,
                            method
                    );

                    AIAnalysisResult aiResult =
                            aiService.analyzeMethod(code);

                    results.add(
                            new MethodRisk(method, aiResult)
                    );

                } catch (Exception e) {
                    System.out.println("AI analysis failed for " + method);
                }

            }

        }

        results.sort(
                Comparator.comparingInt(
                                (MethodRisk r) ->
                                        r.result.getRiskScore())
                        .reversed()
        );

        return results;
    }

    public static class MethodRisk {

        public String method;
        public AIAnalysisResult result;

        public MethodRisk(String method,
                          AIAnalysisResult result) {
            this.method = method;
            this.result = result;
        }
    }
}