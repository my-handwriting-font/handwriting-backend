package com.antaehoo.handwriting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 회원 탈퇴 요청. 본인 확인을 위해 비밀번호를 한 번 더 받는다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {
    private Long userId;
    private String password;
}