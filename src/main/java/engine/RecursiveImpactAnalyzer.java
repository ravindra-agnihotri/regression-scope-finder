package engine;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class RecursiveImpactAnalyzer {

    private final UsageFinder usageFinder;

    public RecursiveImpactAnalyzer(UsageFinder usageFinder) {
        this.usageFinder = usageFinder;
    }

   public Set<MethodReference> findAllImpactedClasses(String repoPath,
                                              Set<String> initialMethods) {

        Set<MethodReference> visitedClasses = new HashSet<>();
        Set<String> methodsToProcess = new HashSet<>(initialMethods);

        while (!methodsToProcess.isEmpty()) {

            Set<String> nextLevelMethods = new HashSet<>();

            for (String method : methodsToProcess) {

                Set<MethodReference> callers =
                        usageFinder.findMethodUsage(repoPath, method);

                for (MethodReference callerClass : callers) {

                    if (!visitedClasses.contains(callerClass)) {
                        visitedClasses.add(callerClass);

                        // Optional: extract caller methods later
                        // For now we just propagate class-level impact
                    }
                }
            }

            methodsToProcess = nextLevelMethods; // currently level-1 only
        }

        return visitedClasses;
    }


    public String extractFullMethodCode(String filePath,
                                        String methodName) {

        try {
            CompilationUnit cu =
                    StaticJavaParser.parse(new File(filePath));

            for (MethodDeclaration method :
                    cu.findAll(MethodDeclaration.class)) {

                if (method.getNameAsString()
                        .equals(methodName)) {

                    return method.toString();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error extracting method body", e);
        }

        return null;
    }
}