package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증번호 발송 / 확인.
 *
 * 1) sendCode: 6자리 번호를 만들어 메일로 보낸다 (5분 동안 유효, 60초 안에 다시 보내기 불가)
 * 2) verifyCode: 번호가 맞으면 그 이메일을 "인증됨"으로 표시한다 (30분 동안 유효)
 * 3) 회원가입 때 isVerified로 확인하고, 가입이 끝나면 consume으로 지운다
 *
 * 졸작 규모라 DB 대신 서버 메모리에 보관한다 (서버를 재시작하면 지워진다).
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_WAIT = Duration.ofSeconds(60);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username}")
    private String fromAddress;

    private record PendingCode(String code, Instant sentAt, Instant expiresAt) {}

    /** 이메일 → 보낸 인증번호 */
    private final Map<String, PendingCode> pending = new ConcurrentHashMap<>();
    /** 이메일 → 인증 유효 시각 */
    private final Map<String, Instant> verified = new ConcurrentHashMap<>();

    /** 인증번호를 보낸다. 문제가 있으면 IllegalArgumentException(화면에 보여줄 문구). */
    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (!email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        if (userRepository.existsByUserEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Instant now = Instant.now();
        PendingCode prev = pending.get(email);
        if (prev != null && now.isBefore(prev.sentAt().plus(RESEND_WAIT))) {
            long left = Duration.between(now, prev.sentAt().plus(RESEND_WAIT)).getSeconds() + 1;
            throw new IllegalArgumentException(left + "초 후에 다시 요청해 주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[손글씨 폰트] 이메일 인증번호");
        message.setText("인증번호는 " + code + " 입니다.\n5분 안에 회원가입 화면에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            System.out.println("[메일 발송 실패] " + e.getMessage()); // 원인 확인용
            throw new IllegalArgumentException("메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        pending.put(email, new PendingCode(code, now, now.plus(CODE_TTL)));
        verified.remove(email);
    }

    /** 인증번호 확인. 맞으면 인증됨으로 표시한다. */
    public void verifyCode(String rawEmail, String rawCode) {
        String email = normalize(rawEmail);
        String code = rawCode == null ? "" : rawCode.trim();

        PendingCode p = pending.get(email);
        if (p == null) {
            throw new IllegalArgumentException("먼저 인증번호를 받아 주세요.");
        }
        if (Instant.now().isAfter(p.expiresAt())) {
            pending.remove(email);
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 받아 주세요.");
        }
        if (!p.code().equals(code)) {
            throw new IllegalArgumentException("인증번호가 올바르지 않습니다.");
        }

        pending.remove(email);
        verified.put(email, Instant.now().plus(VERIFIED_TTL));
    }

    /** 회원가입 직전 확인: 인증을 마쳤고 아직 유효한가 */
    public boolean isVerified(String rawEmail) {
        Instant until = verified.get(normalize(rawEmail));
        return until != null && Instant.now().isBefore(until);
    }

    /** 가입이 끝나면 인증 기록을 지운다 (같은 인증으로 두 번 가입 방지) */
    public void consume(String rawEmail) {
        verified.remove(normalize(rawEmail));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}