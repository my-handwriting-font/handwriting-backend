package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.dto.StrokeData;
import com.antaehoo.handwriting.repository.Consonant;
import com.antaehoo.handwriting.repository.ConsonantRepository;
import com.antaehoo.handwriting.repository.Vowel;
import com.antaehoo.handwriting.repository.VowelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 폰트 만들기용: 사용자가 검증을 통과해 저장한 자모의 원본 좌표를 한 번에 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class JamoService {

    private final ConsonantRepository consonantRepository;
    private final VowelRepository vowelRepository;

    /**
     * 글자 → 원본 좌표. 원본 좌표가 없는 예전 데이터는 뺀다.
     * 같은 글자가 여러 개면 가장 최근 것(나중에 나온 것)으로 덮어쓴다.
     */
    @Transactional(readOnly = true)
    public Map<String, List<StrokeData>> getRawJamo(Long userId) {
        Map<String, List<StrokeData>> result = new LinkedHashMap<>();

        for (Consonant c : consonantRepository.findByUser_UserIdOrderByConsonantIdAsc(userId)) {
            if (c.getRawVectorMap() != null && !c.getRawVectorMap().isEmpty()) {
                result.put(String.valueOf(c.getCharName()), c.getRawVectorMap());
            }
        }
        for (Vowel v : vowelRepository.findByUser_UserIdOrderByVowelIdAsc(userId)) {
            if (v.getRawVectorMap() != null && !v.getRawVectorMap().isEmpty()) {
                result.put(String.valueOf(v.getCharName()), v.getRawVectorMap());
            }
        }
        return result;
    }
}