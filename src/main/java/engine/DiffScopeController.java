package engine;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diff-scope")
public class DiffScopeController {

    private final DiffScopeService service;

    public DiffScopeController(DiffScopeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<DiffScopeService.DiffScopeResponse> analyze(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "master") String baseBranch,
            @RequestParam(defaultValue = "feature") String targetBranch
    ) {
        return ResponseEntity.ok(service.analyze(repoPath, baseBranch, targetBranch));
    }
}
