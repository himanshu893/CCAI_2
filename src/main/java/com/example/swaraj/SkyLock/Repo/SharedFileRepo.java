package com.example.swaraj.SkyLock.Repo;

import com.example.swaraj.SkyLock.Models.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedFileRepo extends JpaRepository<SharedFile,String> {
}
