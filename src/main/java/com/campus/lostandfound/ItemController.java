package com.campus.lostandfound;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        this.repository = repository;
    }

    // BACKEND FEATURE 1: Search, filter and report items (SQL via Spring Data JPA, validated on server)
    @GetMapping
    public List<ItemView> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {

        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedType = type == null ? "" : type.trim();
        List<Item> items;
        boolean hasSearch = !normalizedSearch.isEmpty();
        boolean hasType = !normalizedType.isEmpty() && !normalizedType.equalsIgnoreCase("ALL");

        if (hasSearch && hasType) {
            items = repository
                    .findByTypeIgnoreCaseAndTitleContainingIgnoreCaseOrTypeIgnoreCaseAndLocationContainingIgnoreCase(
                            normalizedType, normalizedSearch, normalizedType, normalizedSearch);
        } else if (hasSearch) {
            items = repository.findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(
                    normalizedSearch, normalizedSearch);
        } else if (hasType) {
            items = repository.findByTypeIgnoreCase(normalizedType);
        } else {
            items = repository.findAll();
        }

        return items.stream()
                .sorted(Comparator.comparing(Item::getId).reversed())   // newest first
                .map(ItemView::of)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item) {
        // Server-side rule: FOUND reports must include a verification question + answer
        if ("FOUND".equalsIgnoreCase(item.getType())) {
            Map<String, String> errors = new LinkedHashMap<>();
            if (item.getSecurityQuestion() == null || item.getSecurityQuestion().isBlank()) {
                errors.put("securityQuestion", "A verification question is required for found items");
            }
            if (item.getSecurityAnswer() == null || item.getSecurityAnswer().isBlank()) {
                errors.put("securityAnswer", "An expected answer is required for found items");
            }
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }
        }
        item.setId(null);
        item.setType(item.getType().toUpperCase(Locale.ROOT));
        item.setStatus("OPEN");
        item.setFailedAttempts(0);
        return ResponseEntity.status(201).body(ItemView.of(repository.save(item)));
    }

    // BACKEND FEATURE 2: Anti-theft claim verification with 3-attempt lockout
    @PostMapping("/{id}/claim")
    public ResponseEntity<Map<String, Object>> claimItem(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {

        Optional<Item> optionalItem = repository.findById(id);
        if (optionalItem.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Item not found."));
        }

        Item item = optionalItem.get();

        if ("CLAIMED".equals(item.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "This item has already been claimed."));
        }
        if ("LOCKED".equals(item.getStatus())) {
            return ResponseEntity.status(423).body(Map.of(
                    "message", "Too many wrong attempts. This item is locked. Please visit the campus security desk."));
        }

        String submitted = payload == null ? "" : payload.getOrDefault("answer", "").trim();
        if (submitted.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please enter an answer."));
        }

        if (item.getSecurityAnswer() != null &&
                item.getSecurityAnswer().trim().equalsIgnoreCase(submitted)) {
            item.setStatus("CLAIMED");
            repository.save(item);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Verification successful! Contact the finder at: " + item.getContactInfo()));
        }

        item.setFailedAttempts(item.getFailedAttempts() + 1);
        int left = Math.max(0, ItemView.MAX_ATTEMPTS - item.getFailedAttempts());
        if (left == 0) {
            item.setStatus("LOCKED");
        }
        repository.save(item);

        String msg = left == 0
                ? "Incorrect answer. Item is now locked. Please visit the campus security desk."
                : "Incorrect answer. Claim rejected. Attempts left: " + left;
        return ResponseEntity.status(400).body(Map.of("status", "FAILED", "message", msg, "attemptsLeft", left));
    }

    // Extra: live statistics for the dashboard (SQL COUNT queries)
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", repository.count());
        m.put("lost", repository.countByTypeIgnoreCase("LOST"));
        m.put("found", repository.countByTypeIgnoreCase("FOUND"));
        m.put("claimed", repository.countByStatus("CLAIMED"));
        return m;
    }
}
