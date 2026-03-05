package util;

public class Utils {
    public static String extractClassName(String filePath) {

        String cleaned = filePath
                .replace("src/main/java/", "")
                .replace("/", ".")
                .replace(".java", "");

        return cleaned;
    }
}
