package engine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GitCommandExecutor {

    public record CommandResult(int exitCode, List<String> output) {
    }

    public List<String> execute(File repoDir, String... command) {
        return executeWithResult(repoDir, command).output();
    }

    public List<String> executeOrThrow(File repoDir, String... command) {
        CommandResult result = executeWithResult(repoDir, command);

        if (result.exitCode() != 0) {
            throw new RuntimeException("Command failed (" + String.join(" ", command) + "): "
                    + String.join("\n", result.output()));
        }

        return result.output();
    }

    public CommandResult executeWithResult(File repoDir, String... command) {
        List<String> output = new ArrayList<>();

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repoDir);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output);

        } catch (Exception e) {
            throw new RuntimeException("Error executing command", e);
        }
    }
}
