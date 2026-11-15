package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.dto.StrokeData;
import com.antaehoo.handwriting.repository.Font;
import com.antaehoo.handwriting.repository.FontRepository;
import com.antaehoo.handwriting.repository.User;
import com.antaehoo.handwriting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 폰트 만들기 / 저장 / 목록 / 불러오기.
 *
 * makeFromJamo: DB에 저장된 자모 26개 → FastAPI(/make-font-from-jamo)가 서버에서 배치 + TTF 생성
 *               → 파일 저장 + font 테이블 기록.
 * 파일은 {font.storage-dir}/{userId}/ 에 저장한다 (기본: 프로젝트 폴더의 fonts/).
 */
@Service
@RequiredArgsConstructor
public class FontService {

    /** 폰트 만들기에 꼭 필요한 자모 26개 (자음 14 + 모음 12) */
    private static final List<String> REQUIRED_JAMO = List.of(
            "ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ",
            "ㅏ","ㅐ","ㅑ","ㅓ","ㅔ","ㅕ","ㅗ","ㅛ","ㅜ","ㅠ","ㅡ","ㅣ"
    );

    private final FontRepository fontRepository;
    private final UserRepository userRepository;
    private final JamoService jamoService;
    private final RestClient fastApiRestClient;

    @Value("${font.storage-dir:fonts}")
    private String storageDir;

    /**
     * 서버에서 폰트 만들기.
     * DB의 자모 26개를 FastAPI로 보내 배치(1~6번 규칙) + TTF를 받아 저장한다.
     */
    public Font makeFromJamo(Long userId, String fontName, Double strokeWidth) throws IOException {
        if (userId == null) throw new IllegalArgumentException("로그인 정보가 없습니다.");
        String name = (fontName == null || fontName.isBlank()) ? "내손글씨" : fontName.trim();
        double width = strokeWidth == null ? 60.0 : strokeWidth;

        Map<String, List<StrokeData>> jamo = jamoService.getRawJamo(userId);
        List<String> missing = REQUIRED_JAMO.stream().filter(j -> !jamo.containsKey(j)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("자모 26개를 먼저 저장해 주세요. 빠진 자모: " + String.join(", ", missing));
        }

        ResponseEntity<byte[]> response;
        try {
            response = fastApiRestClient.post()
                    .uri("/make-font-from-jamo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("fontName", name, "strokeWidth", width, "jamo", jamo))
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientResponseException e) {
            throw new IllegalArgumentException("TTF 생성 실패 (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new IllegalArgumentException("FastAPI 서버에 연결하지 못했습니다.");
        }

        byte[] ttf = response.getBody();
        Integer glyphCount = null;
        String count = response.getHeaders().getFirst("X-Glyph-Count");
        if (count != null) {
            try {
                glyphCount = Integer.parseInt(count);
            } catch (NumberFormatException ignored) {
            }
        }
        return save(userId, name, width, glyphCount, ttf);
    }

    @Transactional
    public Font save(Long userId, String fontName, Double strokeWidth, Integer glyphCount, byte[] ttf)
            throws IOException {
        if (ttf == null || ttf.length < 12) {
            throw new IllegalArgumentException("TTF 파일이 비어 있습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("없는 사용자입니다."));

        String name = (fontName == null || fontName.isBlank()) ? "내손글씨" : fontName.trim();
        // 파일 이름에 쓸 수 없는 문자는 _ 로
        String safeName = name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");

        Path dir = Paths.get(storageDir, String.valueOf(userId)).toAbsolutePath();
        Files.createDirectories(dir);
        Path file = dir.resolve(System.currentTimeMillis() + "_" + safeName + ".ttf");
        Files.write(file, ttf);

        Font font = new Font();
        font.setUser(user);
        font.setFontName(name);
        font.setFilePath(file.toString());
        font.setStrokeWidth(strokeWidth == null ? 60 : (int) Math.round(strokeWidth));
        font.setGlyphCount(glyphCount);
        return fontRepository.save(font);
    }

    @Transactional(readOnly = true)
    public List<Font> list(Long userId) {
        return fontRepository.findByUser_UserIdOrderByFontIdDesc(userId);
    }

    /** 내 폰트인지 확인하고 돌려준다 (다른 사람 폰트는 못 받게) */
    @Transactional(readOnly = true)
    public Font get(Long fontId, Long userId) {
        Font font = fontRepository.findById(fontId)
                .orElseThrow(() -> new IllegalArgumentException("없는 폰트입니다."));
        if (!font.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("없는 폰트입니다.");
        }
        return font;
    }

    public byte[] readFile(Font font) throws IOException {
        Path path = Paths.get(font.getFilePath());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("서버에 파일이 없습니다.");
        }
        return Files.readAllBytes(path);
    }

    /** 회원 탈퇴: 이 사용자의 폰트 파일과 DB 기록을 모두 지운다 */
    @Transactional
    public void deleteAllOf(Long userId) {
        List<Font> fonts = fontRepository.findByUser_UserIdOrderByFontIdDesc(userId);
        for (Font f : fonts) {
            try {
                Files.deleteIfExists(Paths.get(f.getFilePath()));
            } catch (IOException ignored) {
                // 파일이 이미 없거나 지우지 못해도 DB 기록은 지운다
            }
        }
        fontRepository.deleteAll(fonts);
    }
}