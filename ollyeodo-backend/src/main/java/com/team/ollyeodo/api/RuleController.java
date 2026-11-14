package com.team.ollyeodo.api;

import com.team.ollyeodo.service.RuleQueryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuleController {
    private final RuleQueryService service;

    public RuleController(RuleQueryService service) {
        this.service = service;
    }

    @GetMapping("/api/rules")
    public List<RuleResponse> getRules(@RequestParam(name = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return service.findAll(asOf);
    }
}
