package com.example.swaraj.SkyLock.Services;


import ch.qos.logback.core.util.StringUtil;
import com.example.swaraj.SkyLock.Models.FileEntity;
import com.example.swaraj.SkyLock.Models.Folder;
import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.FileRepo;
import com.example.swaraj.SkyLock.Repo.FolderRepo;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;


@Service
public class FileService {

    private UsersRepo usersRepo;
    private FileRepo fileRepo;
    private FolderRepo folderRepo;

    public FileService(UsersRepo usersRepo, FileRepo fileRepo, FolderRepo folderRepo) {
        this.usersRepo = usersRepo;
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
    }

    @Autowired
    FolderServices folderServices;

    @Value("${file.storage.path}")
    private String storagePath;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    public void createUserStroageFolder(String userId) {

        String basePath = storagePath;
        System.out.println(storagePath);
        String userFolder = storagePath + "user_" + userId;
        System.out.println(userFolder);
        File directory = new File(userFolder);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("directory is created");
        }
    }

    @Transactional
    public String uploadFile(MultipartFile file, String folderId) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth.getName();
        Users user = usersRepo.findByUsername(userName);

        long fileSize = file.getSize();
        if (fileSize > maxFileSize.toBytes()) {
            throw new RuntimeException("File exceeds upload limit");
        }

        if (user.getUsedStorage() + fileSize > user.getMaxStorage()) {
            throw new RuntimeException("Storage Limit is exceeds");
        }

        Path basePath = Path.of(storagePath, "user_" + user.getId());

        Path finalPath = basePath;
        Folder folder = null;
        System.out.println(folderId);
        if(StringUtils.hasText(folderId)){
            folder = folderRepo.findByIdIs(folderId);
            if (folder == null)
                throw new RuntimeException("Folder is not exists");

            if (!folder.getOwner().getId().equals(user.getId()))
                throw new RuntimeException("Unauthorized access");

            String relativePath = folderServices.buildFolderPath(folder);
            finalPath = basePath.resolve(relativePath);
            System.out.println(basePath);

        }
        if (!Files.exists(finalPath)) {
            Files.createDirectories(finalPath);
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        Path filePath = finalPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        FileEntity fileEntity = FileEntity.builder()
                .filename(filename)
                .path(String.valueOf(filePath))
                .size(fileSize)
                .owner(user)
                .folder(folder)
                .uploadedAt(LocalDateTime.now())
                .build();

        fileRepo.save(fileEntity);
        user.setUsedStorage(user.getUsedStorage() + fileSize);
        usersRepo.save(user);

        return "File uploaded successfully";
    }
}
