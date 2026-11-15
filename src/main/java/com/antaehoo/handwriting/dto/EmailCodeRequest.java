package com.antaehoo.handwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 이메일 인증 요청. 인증번호 보내기에는 email만, 확인에는 email + code를 쓴다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailCodeRequest {
    private String email;
    private String code;
}