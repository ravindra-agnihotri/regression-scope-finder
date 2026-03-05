package engine;

import java.util.List;

public class AIAnalysisResult {

    private String summary;
    private String risk;
    private String reason;
    private List<String> test_focus;

    public String getSummary() { return summary; }
    public String getRisk() { return risk; }
    public String getReason() { return reason; }
    public List<String> getTest_focus() { return test_focus; }

    public int getRiskScore() {
        if (risk == null) return 0;

        return switch (risk.toUpperCase()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return """
================ AI ANALYSIS ================
Summary:
%s

Risk: %s

Reason:
%s

Suggested Test Focus:
%s
=============================================
""".formatted(summary, risk, reason, test_focus);
    }
}