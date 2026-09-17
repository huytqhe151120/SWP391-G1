package com.swp391.g1.controller;

import com.swp391.g1.entity.ExtracurricularActivity;
import com.swp391.g1.service.ExtracurricularActivityService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/extracurricular-activities")
public class ExtracurricularActivityController {

    private final ExtracurricularActivityService service;

    public ExtracurricularActivityController(
            ExtracurricularActivityService service
    ) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExtracurricularActivity> create(
            @RequestBody ExtracurricularActivity activity
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(activity));
    }

    @GetMapping
    public ResponseEntity<List<ExtracurricularActivity>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtracurricularActivity> findById(
            @PathVariable Long id
    ) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExtracurricularActivity> update(
            @PathVariable Long id,
            @RequestBody ExtracurricularActivity activity
    ) {
        return ResponseEntity.ok(service.update(id, activity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleEntityNotFound() {
        return ResponseEntity.notFound().build();
    }
}
