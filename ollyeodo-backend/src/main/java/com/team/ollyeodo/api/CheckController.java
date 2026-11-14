package com.team.ollyeodo.api;

import com.team.ollyeodo.service.CheckService;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/checks")
public class CheckController {
    private final CheckService service;

    public CheckController(CheckService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CheckResponse> create(@RequestPart("realImage") MultipartFile realImage,
            @RequestPart("aiImage") MultipartFile aiImage, @RequestParam("caption") String caption,
            @RequestParam(value = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        CheckResponse response = service.create(realImage, aiImage, caption, asOf);
        return ResponseEntity.created(URI.create("/api/checks/" + response.checkId())).body(response);
    }

    @GetMapping("/{id}")
    public CheckResponse get(@PathVariable("id") long id) {
        return service.get(id);
    }
}
