package engine;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MethodExtractor {

    public List<String> extractMethodNames(String filePath) {

        List<String> methods = new ArrayList<>();

        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));

            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                methods.add(method.getNameAsString());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error parsing file: " + filePath, e);
        }

        return methods;
    }
}