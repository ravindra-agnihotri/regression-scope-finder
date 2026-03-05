package engine;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class MethodImpactAnalyzer {

    // 🔎 Find impacted methods based on changed lines
    public Set<String> findImpactedMethods(String fileFullPath,
                                           Set<Integer> changedLines) {

        Set<String> impactedMethods = new HashSet<>();

        try {
            CompilationUnit cu =
                    StaticJavaParser.parse(new File(fileFullPath));

            for (MethodDeclaration method :
                    cu.findAll(MethodDeclaration.class)) {

                if (method.getBegin().isPresent()
                        && method.getEnd().isPresent()) {

                    int start = method.getBegin().get().line;
                    int end = method.getEnd().get().line;

                    for (int changedLine : changedLines) {

                        if (changedLine >= start
                                && changedLine <= end) {

                            impactedMethods.add(
                                    method.getNameAsString()
                            );
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error analyzing methods", e);
        }

        return impactedMethods;
    }

    // 🤖 Extract full method body for AI analysis
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