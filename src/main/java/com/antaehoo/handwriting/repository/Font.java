package com.antaehoo.handwriting.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 사용자가 만든 TTF. 파일은 서버 폴더에 두고 DB에는 경로만 저장한다. */
@Entity
@Table(name = "font")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Font {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "font_id")
    private Long fontId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "font_name", nullable = false, length = 100)
    private String fontName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    // 획 두께 (글자 한 칸 = 1000 기준, 30~120)
    @Column(name = "stroke_width", nullable = false)
    private Integer strokeWidth;

    @Column(name = "glyph_count")
    private Integer glyphCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (strokeWidth == null) strokeWidth = 60;
    }
}