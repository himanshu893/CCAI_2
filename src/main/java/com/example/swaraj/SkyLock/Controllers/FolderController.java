package com.example.swaraj.SkyLock.Controllers;

import com.example.swaraj.SkyLock.Services.FolderServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class FolderController {

    private final FolderServices folderServices;

    public FolderController(FolderServices folderServices) {
        this.folderServices = folderServices;
    }

    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String parentId = request.get("parentId");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Folder name is required"));
        }

        try {
            String message = folderServices.createFolder(name.trim(), parentId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
