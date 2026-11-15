package com.antaehoo.handwriting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsonantRepository extends JpaRepository<Consonant, Long> {
    Consonant findByCharName(char charName);

    /** 이 사용자의 이 자음 중 가장 최근 것 (같은 글자가 여러 개 있어도 안전) */
    Optional<Consonant> findFirstByUser_UserIdAndCharNameOrderByConsonantIdDesc(Long userId, char charName);

    /** 이 사용자의 자음 전체 (오래된 것부터) */
    List<Consonant> findByUser_UserIdOrderByConsonantIdAsc(Long userId);

    /** 회원 탈퇴: 이 사용자의 자음 전부 삭제 */
    void deleteByUser_UserId(Long userId);
}