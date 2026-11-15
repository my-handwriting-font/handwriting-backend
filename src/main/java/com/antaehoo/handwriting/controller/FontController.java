package com.antaehoo.handwriting.controller;

import com.antaehoo.handwriting.dto.FontMakeRequest;
import com.antaehoo.handwriting.repository.Font;
import com.antaehoo.handwriting.service.FontService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/font")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FontController {

    private final FontService fontService;

    /** 목록에 내려줄 정보 (파일 경로는 숨긴다) */
    public record FontInfo(Long fontId, String fontName, Integer strokeWidth,
                           Integer glyphCount, LocalDateTime createdAt) {
        static FontInfo of(Font f) {
            return new FontInfo(f.getFontId(), f.getFontName(), f.getStrokeWidth(),
                    f.getGlyphCount(), f.getCreatedAt());
        }
    }

    /**
     * 서버에서 폰트 만들기 (배치 + TTF + 저장).
     * POST /api/font/make   { "userId": 3, "fontName": "내손글씨", "strokeWidth": 60 }
     * 성공: { "fontId": 5, "glyphCount": 11223 } / 실패: 400 { "message": "..." }
     */
    @PostMapping("/make")
    public ResponseEntity<?> make(@RequestBody FontMakeRequest request) {
        try {
            Font font = fontService.makeFromJamo(
                    request.getUserId(), request.getFontName(), request.getStrokeWidth());
            Map<String, Object> body = new HashMap<>();
            body.put("fontId", font.getFontId());
            body.put("glyphCount", font.getGlyphCount()); // null일 수 있어서 Map.of 대신 HashMap
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "파일을 저장하지 못했습니다."));
        }
    }

    /**
     * (예전 방식) Flutter에서 만든 TTF를 그대로 저장. 본문은 TTF 바이트 (application/octet-stream).
     * 지금 화면에서는 쓰지 않지만 남겨 둔다.
     */
    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> save(@RequestParam Long userId,
                                  @RequestParam(defaultValue = "내손글씨") String fontName,
                                  @RequestParam(required = false) Double strokeWidth,
                                  @RequestParam(required = false) Integer glyphCount,
                                  @RequestBody byte[] ttf) {
        try {
            Font font = fontService.save(userId, fontName, strokeWidth, glyphCount, ttf);
            return ResponseEntity.ok(Map.of("fontId", font.getFontId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "파일을 저장하지 못했습니다."));
        }
    }

    /** 내 폰트 목록. GET /api/font?userId=3 */
    @GetMapping
    public List<FontInfo> list(@RequestParam Long userId) {
        return fontService.list(userId).stream().map(FontInfo::of).toList();
    }

    /** TTF 다운로드. GET /api/font/5/file?userId=3 */
    @GetMapping("/{fontId}/file")
    public ResponseEntity<?> download(@PathVariable Long fontId, @RequestParam Long userId) {
        try {
            Font font = fontService.get(fontId, userId);
            byte[] bytes = fontService.readFile(font);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("font/ttf"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(font.getFontName() + ".ttf", StandardCharsets.UTF_8)
                            .build().toString())
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "파일을 읽지 못했습니다."));
        }
    }
}