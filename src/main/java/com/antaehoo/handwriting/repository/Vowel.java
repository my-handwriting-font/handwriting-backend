package com.antaehoo.handwriting.repository;

import com.antaehoo.handwriting.dto.StrokeData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name="vowel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vowel {

    @Id //pk
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vowel_id")
    private Long vowelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "char_name", nullable = false, length = 1)
    private char charName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vector_map", columnDefinition = "json")
    private List<StrokeData> vectorMap; // 정규화 좌표 (검증용)

    @Column(name = "char_count", nullable = true, unique = false)
    private int charCount;

    // 캔버스에 쓴 원본 좌표 (정규화 전). 폰트 합성에 원본 가로세로 비율이 필요해서 따로 저장한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_vector_map", columnDefinition = "json")
    private List<StrokeData> rawVectorMap;
}