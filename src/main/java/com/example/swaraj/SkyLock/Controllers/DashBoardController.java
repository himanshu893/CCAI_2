package com.example.swaraj.SkyLock.Controllers;

import com.example.swaraj.SkyLock.Models.FileEntity;
import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.FileRepo;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import com.example.swaraj.SkyLock.Services.DashBoardService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class DashBoardController {

    private final DashBoardService dashBoardService;
    private final FileRepo fileRepo;
    private final UsersRepo usersRepo;

    public DashBoardController(DashBoardService dashBoardService, FileRepo fileRepo, UsersRepo usersRepo) {
        this.dashBoardService = dashBoardService;
        this.fileRepo = fileRepo;
        this.usersRepo = usersRepo;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam(required = false) String folderId) {
        Map<String, Object> data = dashBoardService.getDashboardData(folderId);
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = usersRepo.findByUsername(auth.getName());

        Optional<FileEntity> optFile = fileRepo.findById(fileId);
        if (optFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileEntity file = optFile.get();
        if (!file.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        // Delete physical file
        try {
            Path filePath = Path.of(file.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but continue to remove DB record
        }

        // Update storage
        user.setUsedStorage(user.getUsedStorage() - file.getSize());
        usersRepo.save(user);

        fileRepo.delete(file);

        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileId) throws MalformedURLException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = usersRepo.findByUsername(auth.getName());

        Optional<FileEntity> optFile = fileRepo.findById(fileId);
        if (optFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileEntity file = optFile.get();
        if (!file.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        Path filePath = Path.of(file.getPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFilename() + "\"")
                .body(resource);
    }
}
