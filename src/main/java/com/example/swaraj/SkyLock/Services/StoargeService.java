package com.example.swaraj.SkyLock.Services;

import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class StoargeService {

    public StoargeService(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;
    }

    private UsersRepo usersRepo;

    public Long getStorage(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users user = usersRepo.findByUsername(auth.getName());
        return user.getUsedStorage();
    }
}
