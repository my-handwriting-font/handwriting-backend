package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.dto.FastApiPredictResponse;
import com.antaehoo.handwriting.dto.HandwritingRequest;
import com.antaehoo.handwriting.dto.JamoSaveAllRequest;
import com.antaehoo.handwriting.dto.StrokeData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HandwritingVerificationService {

    private final VowelService vowelService;
    private final ConsonantService consonantService;
    private final RestClient fastApiRestClient;

    @Value("${handwriting.similarity-threshold:0.6}")
    private double similarityThreshold;

    private static final List<Character> VOWELS = List.of(
            'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ','ㅛ',
            'ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
    );

    /** 폰트 만들기에 꼭 필요한 자모 26개 (자음 14 + 모음 12) */
    private static final List<String> REQUIRED_JAMO = List.of(
            "ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ",
            "ㅏ","ㅐ","ㅑ","ㅓ","ㅔ","ㅕ","ㅗ","ㅛ","ㅜ","ㅠ","ㅡ","ㅣ"
    );

    public boolean isVowel(char c) {
        return VOWELS.contains(c);
    }

    /**
     * 자모 한 개 검증만 한다 (DB에 저장하지 않는다).
     * CNN 인식 글자 == 쓰려던 글자 AND 코사인 유사도 >= 기준값 이면 통과.
     */
    public boolean verify(HandwritingRequest request) {
        FastApiPredictResponse prediction = fastApiRestClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FastApiPredictResponse.class);

        if (prediction == null || prediction.getPredictedChar() == null
                || prediction.getSimilarity() == null) {
            return false;
        }
        return request.getCharName().equals(prediction.getPredictedChar())
                && prediction.getSimilarity() >= similarityThreshold;
    }

    /**
     * 26개를 한꺼번에 저장한다. 하나라도 빠졌으면 아무것도 저장하지 않는다.
     * 중간에 실패하면 @Transactional 때문에 전부 되돌린다.
     */
    @Transactional
    public void saveAll(JamoSaveAllRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        Map<String, List<StrokeData>> jamo = request.getJamo();
        if (jamo == null) {
            throw new IllegalArgumentException("저장할 자모가 없습니다.");
        }
        List<String> missing = REQUIRED_JAMO.stream()
                .filter(j -> jamo.get(j) == null || jamo.get(j).isEmpty())
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("아직 쓰지 않은 자모가 있습니다: " + String.join(", ", missing));
        }

        for (String j : REQUIRED_JAMO) {
            HandwritingRequest one = new HandwritingRequest(j, jamo.get(j), request.getUserId());
            if (isVowel(j.charAt(0))) {
                vowelService.saveNormalization(one);
            } else {
                consonantService.saveNormalization(one);
            }
        }
    }
}