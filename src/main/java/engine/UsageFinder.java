package engine;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import util.Utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class UsageFinder {

    public Set<MethodReference> findMethodUsage(
            String repoPath,
            String targetMethodName
    ) {

        Set<MethodReference> result = new HashSet<>();

        Path rootPath = Paths.get(repoPath).toAbsolutePath();

        scan(new File(repoPath), rootPath, targetMethodName, result);

        return result;
    }

    private void scan(File file,
                      Path rootPath,
                      String targetMethodName,
                      Set<MethodReference> result) {

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                scan(child, rootPath, targetMethodName, result);
            }
            return;
        }

        if (!file.getName().endsWith(".java")) return;

        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            for (MethodDeclaration method :
                    cu.findAll(MethodDeclaration.class)) {

                for (MethodCallExpr call :
                        method.findAll(MethodCallExpr.class)) {

                    if (call.getNameAsString()
                            .equals(targetMethodName)) {

                        Path filePath =
                                file.toPath().toAbsolutePath();

                        Path relative =
                                rootPath.relativize(filePath);

                        String normalized =
                                relative.toString()
                                        .replace("\\", "/");

                        String fqcn =
                                Utils.extractClassName(normalized);

                        result.add(new MethodReference(
                                fqcn,
                                method.getNameAsString()
                        ));
                    }
                }
            }

        } catch (Exception ignored) {
        }
    }
}