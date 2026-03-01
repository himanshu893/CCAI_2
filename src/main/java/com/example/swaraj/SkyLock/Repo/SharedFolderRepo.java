package com.example.swaraj.SkyLock.Repo;

import com.example.swaraj.SkyLock.Models.SharedFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedFolderRepo extends JpaRepository<SharedFolder,String> {
}
