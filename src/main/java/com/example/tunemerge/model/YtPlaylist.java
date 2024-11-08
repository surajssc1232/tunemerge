package com.example.tunemerge.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "yt_playlists")
public class YtPlaylist {
    @Id
    @Column(name = "id", nullable = false)
    private String playlistId;  // YouTube's playlist ID
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description")
    private String description;
    
   
  

   
} 