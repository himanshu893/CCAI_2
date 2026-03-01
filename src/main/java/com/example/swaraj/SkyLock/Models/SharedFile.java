package com.example.swaraj.SkyLock.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @ManyToOne
    @JoinColumn(name = "shared_with_user_id")
    private Users sharedwith;

    private LocalDateTime sharedAt;
}
