package com.example.swaraj.SkyLock.Repo;

import com.example.swaraj.SkyLock.Models.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepo extends JpaRepository<Folder, String> {

    Folder findByIdIs(String id);

    @Query("""
                SELECT f
                FROM Folder f
                WHERE f.parent.id = :parentId
                AND f.owner.id = :userId
            """)
    List<Folder> findFolderByIdAndUser(
            @Param("parentId") String parentId,
            @Param("userId") String userId);

    String findByName(String name);

    List<Folder> findByOwnerIdAndParentIsNull(String ownerId);

    List<Folder> findByOwnerId(String ownerId);
}
