package com.antaehoo.handwriting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FontRepository extends JpaRepository<Font, Long> {

        /** 내 폰트 목록 (최근 것부터) */
        List<Font> findByUser_UserIdOrderByFontIdDesc(Long userId);
}