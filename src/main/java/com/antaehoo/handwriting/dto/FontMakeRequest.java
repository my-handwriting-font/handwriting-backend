package com.antaehoo.handwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 서버에서 폰트 만들기 요청 (자모는 DB에서 꺼낸다) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FontMakeRequest {
    private Long userId;
    private String fontName;
    private Double strokeWidth;
}