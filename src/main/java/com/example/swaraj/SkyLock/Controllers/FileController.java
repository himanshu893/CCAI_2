package com.example.swaraj.SkyLock.Controllers;

import com.example.swaraj.SkyLock.Services.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadPage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String folderId) throws IOException {
        return ResponseEntity.ok(fileService.uploadFile(file, folderId));
    }
}
