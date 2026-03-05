package engine;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class GitDiffService {

    private final GitCommandExecutor executor = new GitCommandExecutor();

    public List<String> getChangedJavaFiles(String repoPath,
                                            String baseBranch,
                                            String targetBranch) {

        File repoDir = new File(repoPath);

        // Always fetch latest remote refs
        executor.execute(repoDir, "git", "fetch", "--all");

        List<String> result = executor.execute(
                repoDir,
                "git", "diff", "--name-only",
                "origin/" + baseBranch + "..origin/" + targetBranch
        );

        return result.stream()
                .filter(file -> file.endsWith(".java"))
                .collect(Collectors.toList());
    }
}