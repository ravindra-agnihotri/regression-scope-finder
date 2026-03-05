package engine;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiffLineExtractor {

    private static final Pattern HUNK_PATTERN =
            Pattern.compile("\\+([0-9]+)(?:,([0-9]+))?");

    public Set<Integer> extractChangedLines(String repoPath,
                                            String baseBranch,
                                            String targetBranch,
                                            String filePath) {

        Set<Integer> changedLines = new HashSet<>();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "git", "diff", "-U0",
                    "origin/" + baseBranch + "..origin/" + targetBranch,
                    "--", filePath
            );

            builder.directory(new File(repoPath));
            builder.redirectErrorStream(true);

            Process process = builder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.startsWith("@@")) {
                    Matcher matcher = HUNK_PATTERN.matcher(line);

                    if (matcher.find()) {
                        int startLine = Integer.parseInt(matcher.group(1));
                        int lineCount = matcher.group(2) != null
                                ? Integer.parseInt(matcher.group(2))
                                : 1;

                        for (int i = 0; i < lineCount; i++) {
                            changedLines.add(startLine + i);
                        }
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            throw new RuntimeException("Error extracting diff lines", e);
        }

        return changedLines;
    }
}