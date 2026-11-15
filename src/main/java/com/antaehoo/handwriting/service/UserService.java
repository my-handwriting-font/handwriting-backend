/*
registerUser -> 회원가입. 아이디 중복 시 예외처리
signup       -> 회원가입 화면용. 입력 검사 + 이메일 인증 확인 + 비밀번호 BCrypt 암호화
login        -> 로그인. 아이디/비밀번호 확인 후 사용자 반환
withdraw     -> 회원 탈퇴. 비밀번호 확인 후 폰트 → 자모 → 사용자 순서로 삭제
 */

package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.dto.LoginRequest;
import com.antaehoo.handwriting.dto.SignupRequest;
import com.antaehoo.handwriting.dto.WithdrawRequest;
import com.antaehoo.handwriting.repository.ConsonantRepository;
import com.antaehoo.handwriting.repository.User;
import com.antaehoo.handwriting.repository.UserRepository;
import com.antaehoo.handwriting.repository.VowelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UserRepository userRepository;
    private final ConsonantRepository consonantRepository;
    private final VowelRepository vowelRepository;
    private final FontService fontService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    UserService(UserRepository userRepository,
                ConsonantRepository consonantRepository,
                VowelRepository vowelRepository,
                FontService fontService,
                EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.consonantRepository = consonantRepository;
        this.vowelRepository = vowelRepository;
        this.fontService = fontService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public void registerUser(User user) {

        if(userRepository.existsByUserLoginId(user.getUserLoginId())) {
            throw new IllegalArgumentException("아이디 중복입니다.");
        }

        userRepository.save(user);
    }

    /** 회원가입 (이메일 인증 필수). 문제가 있으면 IllegalArgumentException(화면에 보여줄 문구). */
    @Transactional
    public Long signup(SignupRequest req) {
        String loginId = trim(req.getLoginId());
        String password = req.getPassword() == null ? "" : req.getPassword();
        String name = trim(req.getName());
        String email = trim(req.getEmail()).toLowerCase();

        if (loginId.isEmpty()) throw new IllegalArgumentException("아이디를 입력하세요.");
        if (password.length() < 4) throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        if (name.isEmpty()) throw new IllegalArgumentException("이름을 입력하세요.");
        if (!email.matches(EMAIL_PATTERN)) throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        if (!emailVerificationService.isVerified(email)) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해 주세요.");
        }
        if (userRepository.existsByUserEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = new User();
        user.setUserLoginId(loginId);
        user.setUserPassword(passwordEncoder.encode(password)); // 평문 대신 BCrypt 해시 저장
        user.setUserName(name);
        user.setUserEmail(email);

        registerUser(user); // 아이디 중복 검사 후 저장
        emailVerificationService.consume(email);
        return user.getUserId();
    }

    /** 로그인. 아이디가 없거나 비밀번호가 틀리면 같은 문구로 실패시킨다. */
    @Transactional(readOnly = true)
    public User login(LoginRequest req) {
        String loginId = trim(req.getLoginId());
        String password = req.getPassword() == null ? "" : req.getPassword();

        User user = userRepository.findByUserLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getUserPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    /**
     * 회원 탈퇴. 비밀번호가 맞으면 이 사용자의 폰트(파일 + DB), 자음, 모음, 계정을 모두 지운다.
     * consonant/vowel/font가 user를 참조(FK)하므로 사용자보다 먼저 지운다.
     */
    @Transactional
    public void withdraw(WithdrawRequest req) {
        if (req.getUserId() == null) throw new IllegalArgumentException("로그인 정보가 없습니다.");
        String password = req.getPassword() == null ? "" : req.getPassword();

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("없는 사용자입니다."));
        if (!passwordEncoder.matches(password, user.getUserPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        Long userId = user.getUserId();
        fontService.deleteAllOf(userId);
        consonantRepository.deleteByUser_UserId(userId);
        vowelRepository.deleteByUser_UserId(userId);
        userRepository.delete(user);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}