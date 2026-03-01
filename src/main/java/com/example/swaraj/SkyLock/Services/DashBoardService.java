package com.example.swaraj.SkyLock.Services;

import com.example.swaraj.SkyLock.Models.FileEntity;
import com.example.swaraj.SkyLock.Models.Folder;
import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.FileRepo;
import com.example.swaraj.SkyLock.Repo.FolderRepo;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashBoardService {

    private final UsersRepo usersRepo;
    private final FolderRepo folderRepo;
    private final FileRepo fileRepo;

    public DashBoardService(UsersRepo usersRepo, FolderRepo folderRepo, FileRepo fileRepo) {
        this.usersRepo = usersRepo;
        this.folderRepo = folderRepo;
        this.fileRepo = fileRepo;
    }

    public Map<String, Object> getDashboardData(String folderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = usersRepo.findByUsername(auth.getName());

        Map<String, Object> result = new HashMap<>();

        List<FileEntity> files;
        List<Folder> folders;

        if (folderId == null || folderId.isEmpty()) {
            // Root level
            files = fileRepo.findByOwnerIdAndFolderIsNull(user.getId());
            folders = folderRepo.findByOwnerIdAndParentIsNull(user.getId());
        } else {
            files = fileRepo.findByFolderIdAndOwnerId(folderId, user.getId());
            folders = folderRepo.findFolderByIdAndUser(folderId, user.getId());
        }

        // Build file list
        List<Map<String, Object>> fileList = new ArrayList<>();
        for (FileEntity file : files) {
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", file.getId());
            fileMap.put("filename", file.getFilename());
            fileMap.put("size", file.getSize());
            fileMap.put("uploadedAt", file.getUploadedAt() != null ? file.getUploadedAt().toString() : "");
            fileMap.put("type", getFileType(file.getFilename()));
            fileList.add(fileMap);
        }

        // Build folder list
        List<Map<String, Object>> folderList = new ArrayList<>();
        for (Folder folder : folders) {
            Map<String, Object> folderMap = new HashMap<>();
            folderMap.put("id", folder.getId());
            folderMap.put("name", folder.getName());
            int fileCount = folder.getFiles() != null ? folder.getFiles().size() : 0;
            int subFolderCount = folder.getSubFolders() != null ? folder.getSubFolders().size() : 0;
            folderMap.put("itemCount", fileCount + subFolderCount);
            folderList.add(folderMap);
        }

        // Storage info
        Map<String, Object> storage = new HashMap<>();
        storage.put("used", user.getUsedStorage());
        storage.put("max", user.getMaxStorage());
        storage.put("percentage", user.getMaxStorage() > 0
                ? Math.round((double) user.getUsedStorage() / user.getMaxStorage() * 100)
                : 0);

        result.put("files", fileList);
        result.put("folders", folderList);
        result.put("storage", storage);
        result.put("username", user.getUsername());

        return result;
    }

    private String getFileType(String filename) {
        if (filename == null)
            return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg")) {
            return "image";
        } else if (lower.endsWith(".pdf")) {
            return "pdf";
        } else if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "document";
        } else if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv")) {
            return "spreadsheet";
        } else if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov")) {
            return "video";
        } else if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac")) {
            return "audio";
        } else if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) {
            return "archive";
        }
        return "file";
    }
}
