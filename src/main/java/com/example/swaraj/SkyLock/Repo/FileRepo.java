package com.example.swaraj.SkyLock.Repo;

import com.example.swaraj.SkyLock.Models.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepo extends JpaRepository<FileEntity, String> {

    @Query("""
                    SELECT f
                    FROM FileEntity f
                    WHERE f.folder.id = :parentId
                    AND f.owner.id = :userId
            """)
    List<FileEntity> findFilesByIdAndUser(
            @Param("parentId") String parentId,
            @Param("userId") String userId);

    List<FileEntity> findByOwnerIdAndFolderIsNull(String ownerId);

    List<FileEntity> findByOwnerId(String ownerId);

    List<FileEntity> findByFolderIdAndOwnerId(String folderId, String ownerId);
}
