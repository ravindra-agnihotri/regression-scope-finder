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

        // Try to fetch latest refs, but continue if network/auth is unavailable.
        executor.executeWithResult(repoDir, "git", "fetch", "--all");

        String resolvedBase = resolveBranchRef(repoDir, baseBranch);
        String resolvedTarget = resolveBranchRef(repoDir, targetBranch);

        String diffRange = buildDiffRange(repoDir, resolvedBase, resolvedTarget);

        List<String> result = executor.execute(
                repoDir,
                "git", "diff", "--name-only",
                diffRange
        );

        return result.stream()
                .filter(file -> file.endsWith(".java"))
                .collect(Collectors.toList());
    }

    private String buildDiffRange(File repoDir, String baseRef, String targetRef) {
        GitCommandExecutor.CommandResult mergeBase = executor.executeWithResult(
                repoDir,
                "git", "merge-base", baseRef, targetRef
        );

        if (mergeBase.exitCode() == 0 && !mergeBase.output().isEmpty()) {
            return mergeBase.output().get(0) + ".." + targetRef;
        }

        return baseRef + ".." + targetRef;
    }

    private String resolveBranchRef(File repoDir, String branch) {
        String[] candidates = new String[]{
                "origin/" + branch,
                branch,
                "refs/heads/" + branch,
                "refs/remotes/origin/" + branch
        };

        for (String candidate : candidates) {
            GitCommandExecutor.CommandResult result = executor.executeWithResult(
                    repoDir,
                    "git", "rev-parse", "--verify", candidate
            );

            if (result.exitCode() == 0) {
                return candidate;
            }
        }

        throw new RuntimeException("Unable to resolve git branch/reference: " + branch);
    }
}
