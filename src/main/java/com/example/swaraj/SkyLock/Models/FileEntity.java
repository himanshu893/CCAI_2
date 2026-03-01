package com.example.swaraj.SkyLock.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String filename;

    private String path;

    private Long size;

    // Owner of file
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Users owner;

    // Folder (nullable = root file)
    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;

    private LocalDateTime uploadedAt;
}