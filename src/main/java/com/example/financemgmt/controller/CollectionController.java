package com.example.financemgmt.controller;

import com.example.financemgmt.dto.CollectionRequest;
import com.example.financemgmt.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {
    private final CollectionService service;
    public CollectionController(CollectionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionRequest create(@Valid @RequestBody CollectionRequest request) {
        service.collect(request);
        return request;
    }
}
