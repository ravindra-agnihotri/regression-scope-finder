package engine;

import util.Utils;

import java.util.*;

public class App {

    public static void main(String[] args) {

        String repoUrl = "https://github.com/ravindra-agnihotri/testNG.git";
        String cloneDir = "./workspace/testNG";

        String baseBranch = "main";
        String targetBranch = "test";

        AIAnalysisService aiService =
                new AIAnalysisService("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");

        GitCloneService cloneService = new GitCloneService();
        cloneService.cloneRepository(repoUrl, cloneDir);

        GitDiffService diffService = new GitDiffService();
        List<String> changedFiles =
                diffService.getChangedJavaFiles(
                        cloneDir, baseBranch, targetBranch);

        DiffLineExtractor diffExtractor = new DiffLineExtractor();
        MethodImpactAnalyzer analyzer = new MethodImpactAnalyzer();

        // 🔥 Store all method risk results
        List<MethodRisk> allResults = new ArrayList<>();

        for (String file : changedFiles) {

            Set<Integer> changedLines =
                    diffExtractor.extractChangedLines(
                            cloneDir, baseBranch, targetBranch, file);

            String fullPath = cloneDir + "/" + file;

            Set<String> impactedMethods =
                    analyzer.findImpactedMethods(fullPath, changedLines);

            String fqcn = Utils.extractClassName(file);

            for (String method : impactedMethods) {

                String methodBody =
                        analyzer.extractFullMethodCode(fullPath, method);

                if (methodBody != null) {

                    AIAnalysisResult result =
                            aiService.analyzeMethod(methodBody);

                    MethodReference ref =
                            new MethodReference(fqcn, method);

                    allResults.add(new MethodRisk(ref, result));
                }
            }
        }

        // 🔥 Sort by risk score (HIGH → LOW)
        allResults.sort(
                Comparator.comparingInt(
                        (MethodRisk m) -> m.result.getRiskScore()
                ).reversed()
        );

        // 🔥 Print prioritized results
        System.out.println("\n===== RISK PRIORITIZED METHODS =====");

        for (MethodRisk mr : allResults) {

            int score = mr.result.getRiskScore();

            String emoji = switch (score) {
                case 3 -> "🔴 HIGH";
                case 2 -> "🟠 MEDIUM";
                case 1 -> "🟢 LOW";
                default -> "⚪ UNKNOWN";
            };

            System.out.println("\n--------------------------------------");
            System.out.println("Method: " + mr.method);
            System.out.println("Risk: " + emoji);
            System.out.println(mr.result);
        }
    }

    // 🔥 Helper wrapper class
    static class MethodRisk {
        MethodReference method;
        AIAnalysisResult result;

        MethodRisk(MethodReference method,
                   AIAnalysisResult result) {
            this.method = method;
            this.result = result;
        }
    }
}