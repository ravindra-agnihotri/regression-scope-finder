package engine;

import java.util.List;
import java.util.Map;

public class AnalysisResponse {

    public List<String> changedFiles;

    public Map<String, Long> riskSummary;

    public List<RegressionAnalysisService.MethodRisk> methods;

    public List<String> suggestions;

}