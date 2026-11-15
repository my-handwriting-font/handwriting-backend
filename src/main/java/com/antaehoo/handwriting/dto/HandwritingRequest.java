package com.antaehoo.handwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandwritingRequest {

    // 플러터에서 보내는 글자 이름 (예: "ㄱ")
    private String charName;

    // 플러터에서 보내는 전체 획 데이터 리스트 (캔버스 원본 좌표)
    private List<StrokeData> strokes;

    // 로그인한 사용자 ID. 없으면(테스트 화면 등) 1번 사용자로 저장한다.
    private Long userId;

}

/*
DTO 구조

HandWritingRequest
-> List<StrokeData> strokes
-> List<PointData> points
-> point Data = double x, y, p
 */