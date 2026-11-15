package com.antaehoo.handwriting.controller;

import com.antaehoo.handwriting.dto.EmailCodeRequest;
import com.antaehoo.handwriting.dto.LoginRequest;
import com.antaehoo.handwriting.dto.LoginResponse;
import com.antaehoo.handwriting.dto.SignupRequest;
import com.antaehoo.handwriting.dto.WithdrawRequest;
import com.antaehoo.handwriting.repository.User;
import com.antaehoo.handwriting.service.EmailVerificationService;
import com.antaehoo.handwriting.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    /** 이메일로 인증번호 보내기. 성공: 200 / 실패: 400 { "message": "..." } */
    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmailCode(@RequestBody EmailCodeRequest request) {
        try {
            emailVerificationService.sendCode(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "인증번호를 보냈습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 인증번호 확인. 성공: 200 / 실패: 400 { "message": "..." } */
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmailCode(@RequestBody EmailCodeRequest request) {
        try {
            emailVerificationService.verifyCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 회원가입 (이메일 인증 필수). 성공: { "userId": 3 } / 실패: 400 { "message": "..." } */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            Long userId = userService.signup(request);
            return ResponseEntity.ok(Map.of("userId", userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 로그인. 성공: { "userId": 3, "userName": "안태후" } / 실패: 401 { "message": "..." } */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request);
            return ResponseEntity.ok(new LoginResponse(user.getUserId(), user.getUserName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    /** 회원 탈퇴. 성공: 200 / 실패: 400 { "message": "..." } */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody WithdrawRequest request) {
        try {
            userService.withdraw(request);
            return ResponseEntity.ok(Map.of("message", "탈퇴되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}