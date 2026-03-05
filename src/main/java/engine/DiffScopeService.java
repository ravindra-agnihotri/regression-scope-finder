package engine;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiffScopeService {

    private static final Pattern HUNK_PATTERN = Pattern.compile("@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,(\\d+))? @@");
    private final GitCommandExecutor executor = new GitCommandExecutor();

    public DiffScopeResponse analyze(String repoPath, String baseBranch, String targetBranch) {
        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            throw new IllegalArgumentException("repoPath must point to a local git repository");
        }

        executor.executeOrThrow(repoDir, "git", "fetch", "--all", "--prune");

        String baseRef = resolveBranchRef(repoDir, baseBranch);
        String targetRef = resolveBranchRef(repoDir, targetBranch);
        String range = baseRef + ".." + targetRef;

        List<String> changedFiles = executor.executeOrThrow(repoDir, "git", "diff", "--name-only", range);

        List<FileDiffResult> fileDiffs = new ArrayList<>();

        for (String filePath : changedFiles) {
            Set<Integer> changedLines = extractChangedLines(repoDir, range, filePath);
            List<String> changedMethods = extractChangedMethods(repoDir, targetRef, filePath, changedLines);

            fileDiffs.add(new FileDiffResult(filePath, changedLines, changedMethods));
        }

        return new DiffScopeResponse(baseRef, targetRef, fileDiffs);
    }

    private String resolveBranchRef(File repoDir, String branch) {
        String remote = "origin/" + branch;
        GitCommandExecutor.CommandResult remoteExists = executor.executeWithResult(
                repoDir, "git", "rev-parse", "--verify", "--quiet", remote
        );

        if (remoteExists.exitCode() == 0) {
            return remote;
        }

        GitCommandExecutor.CommandResult localExists = executor.executeWithResult(
                repoDir, "git", "rev-parse", "--verify", "--quiet", branch
        );

        if (localExists.exitCode() == 0) {
            return branch;
        }

        throw new IllegalArgumentException("Branch not found: " + branch);
    }

    private Set<Integer> extractChangedLines(File repoDir, String range, String filePath) {
        List<String> diffOutput = executor.executeOrThrow(
                repoDir,
                "git", "diff", "-U0", range, "--", filePath
        );

        Set<Integer> changedLines = new TreeSet<>();

        for (String line : diffOutput) {
            Matcher matcher = HUNK_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            int start = Integer.parseInt(matcher.group(1));
            int count = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));

            for (int i = 0; i < count; i++) {
                changedLines.add(start + i);
            }
        }

        return changedLines;
    }

    private List<String> extractChangedMethods(File repoDir,
                                               String targetRef,
                                               String filePath,
                                               Set<Integer> changedLines) {
        if (!filePath.endsWith(".java") || changedLines.isEmpty()) {
            return List.of();
        }

        GitCommandExecutor.CommandResult fileContent = executor.executeWithResult(
                repoDir,
                "git", "show", targetRef + ":" + filePath
        );

        if (fileContent.exitCode() != 0) {
            return List.of();
        }

        String source = String.join("\n", fileContent.output());

        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            Set<String> methodSet = new LinkedHashSet<>();

            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                if (method.getBegin().isEmpty() || method.getEnd().isEmpty()) {
                    continue;
                }

                int start = method.getBegin().get().line;
                int end = method.getEnd().get().line;

                boolean intersects = changedLines.stream().anyMatch(line -> line >= start && line <= end);
                if (intersects) {
                    methodSet.add(method.getDeclarationAsString(false, false, true));
                }
            }

            return new ArrayList<>(methodSet);
        } catch (ParseProblemException ex) {
            return List.of();
        }
    }

    public record DiffScopeResponse(String baseRef,
                                    String targetRef,
                                    int totalFilesChanged,
                                    int totalLinesChanged,
                                    int totalMethodsChanged,
                                    List<FileDiffResult> files) {
        public DiffScopeResponse(String baseRef, String targetRef, List<FileDiffResult> files) {
            this(
                    baseRef,
                    targetRef,
                    files.size(),
                    files.stream().mapToInt(file -> file.changedLines().size()).sum(),
                    files.stream().mapToInt(file -> file.changedMethods().size()).sum(),
                    files
            );
        }
    }

    public record FileDiffResult(String path,
                                 Set<Integer> changedLines,
                                 List<String> changedMethods) {
    }
}
