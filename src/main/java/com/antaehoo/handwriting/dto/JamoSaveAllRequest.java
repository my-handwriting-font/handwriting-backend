package com.antaehoo.handwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** 자모 26개 한꺼번에 저장 요청. jamo: 글자 → 원본 획 좌표 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JamoSaveAllRequest {
    private Long userId;
    private Map<String, List<StrokeData>> jamo;
}