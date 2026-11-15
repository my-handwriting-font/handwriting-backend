package com.antaehoo.handwriting.controller;

import com.antaehoo.handwriting.dto.HandwritingRequest;
import com.antaehoo.handwriting.dto.JamoSaveAllRequest;
import com.antaehoo.handwriting.dto.StrokeData;
import com.antaehoo.handwriting.repository.Consonant;
import com.antaehoo.handwriting.service.ConsonantService;
import com.antaehoo.handwriting.service.HandwritingVerificationService;
import com.antaehoo.handwriting.service.JamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/handwriting")
@CrossOrigin(origins = "*") // 플러터 웹에서 포트가 계속 바뀌어도 통신되도록 전체 개방
@RequiredArgsConstructor
public class HandwritingController {

    private final ConsonantService consonantService;
    private final HandwritingVerificationService handwritingVerificationService;
    private final JamoService jamoService;

    @PostMapping("/save")  //test
    public ResponseEntity<String> saveHandwriting(@RequestBody HandwritingRequest request) {
        consonantService.saveNormalization(request);

        return ResponseEntity.ok("DB 저장 성공!");
    }


    @GetMapping("/latest") //test
    public ResponseEntity<List<StrokeData>> getLatestHandwriting() {
        Consonant latestConsonant = consonantService.getLatestConsonant();

        if (latestConsonant == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latestConsonant.getVectorMap());
    }

    /** 자모 한 개 검증 (CNN + 코사인 유사도). 저장하지 않는다. true / false */
    @PostMapping("/verify")
    public ResponseEntity<Boolean> verify(@RequestBody HandwritingRequest request) {
        return ResponseEntity.ok(handwritingVerificationService.verify(request));
    }

    /**
     * 자모 26개 한꺼번에 저장 (폰트 만들기 화면의 "저장" 버튼)
     * POST /api/handwriting/jamo/save-all   { "userId": 3, "jamo": { "ㄱ": [...], ... } }
     * 성공: 200 / 실패: 400 { "message": "..." }
     */
    @PostMapping("/jamo/save-all")
    public ResponseEntity<?> saveAllJamo(@RequestBody JamoSaveAllRequest request) {
        try {
            handwritingVerificationService.saveAll(request);
            return ResponseEntity.ok(Map.of("message", "저장했습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 폰트 만들기용: 저장된 자모의 원본 좌표 (글자 → 획)
     * GET /api/handwriting/jamo?userId=3
     */
    @GetMapping("/jamo")
    public ResponseEntity<Map<String, List<StrokeData>>> getJamo(
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(jamoService.getRawJamo(userId));
    }
}