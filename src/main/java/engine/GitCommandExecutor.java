package engine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GitCommandExecutor {

    public List<String> execute(File repoDir, String... command) {
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

            process.waitFor();

        } catch (Exception e) {
            throw new RuntimeException("Error executing git command", e);
        }

        return output;
    }
}