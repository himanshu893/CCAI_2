package com.example.swaraj.SkyLock.Services;

import com.example.swaraj.SkyLock.Models.Folder;
import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.FolderRepo;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderServices {

    private final UsersRepo usersRepo;
    private final FolderRepo folderRepo;

    public FolderServices(FolderRepo folderRepo, UsersRepo usersRepo) {
        this.folderRepo = folderRepo;
        this.usersRepo = usersRepo;
    }

    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepo.findByUsername(auth.getName());
    }

    public String buildFolderPath(Folder folder) {
        if (folder.getParent() == null) {
            return folder.getName();
        }
        return buildFolderPath(folder.getParent()) + "/" + folder.getName();
    }

    public String createFolder(String name, String parentId) {
        Users user = getCurrentUser();

        Folder parent = null;
        if (parentId != null && !parentId.isEmpty()) {
            parent = folderRepo.findByIdIs(parentId);
            if (parent == null)
                throw new RuntimeException("Folder is not Found");
            if (!parent.getOwner().getId().equals(user.getId()))
                throw new RuntimeException("User is Not Authorized");
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setParent(parent);
        folder.setOwner(user);

        folderRepo.save(folder);

        return "Folder Created Successfully";
    }

    public List<Folder> viewFolder(String parentName) {
        Users user = getCurrentUser();
        String parentId = folderRepo.findByName(parentName);
        List<Folder> folders = folderRepo.findFolderByIdAndUser(parentId, user.getId());
        return folders;
    }
}
