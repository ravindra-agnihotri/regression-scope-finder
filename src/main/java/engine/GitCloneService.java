package engine;

import org.eclipse.jgit.api.Git;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitCloneService {

    public void cloneRepository(String repoUrl, String cloneDir) {
        File directory = new File(cloneDir);

        try {
            if (directory.exists()) {
                System.out.println("Directory already exists. Deleting...");
                deleteDirectory(directory.toPath());
            }

            System.out.println("Cloning repository...");

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(directory)
                    .call();

            System.out.println("Repository cloned successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Error cloning repository", e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // delete children first
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + p, e);
                    }
                });
    }
}