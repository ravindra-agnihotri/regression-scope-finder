package engine;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.util.*;

public class CallGraphBuilder {

    public Map<String, Set<String>> buildGraph(String repoPath) {

        Map<String, Set<String>> graph = new HashMap<>();

        List<File> javaFiles = new ArrayList<>();

        collectJavaFiles(new File(repoPath), javaFiles);

        for (File file : javaFiles) {

            try {

                CompilationUnit cu =
                        StaticJavaParser.parse(file);

                cu.findAll(MethodDeclaration.class)
                        .forEach(method -> {

                            String methodName =
                                    method.getNameAsString();

                            graph.putIfAbsent(methodName,
                                    new HashSet<>());

                            method.findAll(MethodCallExpr.class)
                                    .forEach(call -> {

                                        String called =
                                                call.getNameAsString();

                                        graph.get(methodName)
                                                .add(called);

                                    });

                        });

            } catch (Exception ignored) {}

        }

        return graph;
    }

    private void collectJavaFiles(File dir,
                                  List<File> files) {

        for (File file : dir.listFiles()) {

            if (file.isDirectory())
                collectJavaFiles(file, files);

            else if (file.getName().endsWith(".java"))
                files.add(file);

        }
    }
}