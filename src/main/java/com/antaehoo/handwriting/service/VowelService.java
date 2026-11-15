package com.antaehoo.handwriting.service;

import com.antaehoo.handwriting.dto.HandwritingRequest;
import com.antaehoo.handwriting.dto.StrokeData;
import com.antaehoo.handwriting.repository.User;
import com.antaehoo.handwriting.repository.UserRepository;
import com.antaehoo.handwriting.repository.Vowel;
import com.antaehoo.handwriting.repository.VowelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VowelService {

    private final VowelRepository vowelRepository;
    private final NormalizationService normalizationService;
    private final UserRepository userRepository;

    @Transactional
    public void registerVowel(Vowel vowel) {
        //테스트 용. 예외처리 나중에
        vowelRepository.save(vowel);
    }

    @Transactional(readOnly = true)
    public Vowel getLatestVowel() {
        List<Vowel> list = vowelRepository.findAll();
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * 검증을 통과한 모음을 저장한다.
     * 같은 사용자의 같은 글자가 이미 있으면 새로 추가하지 않고 덮어쓴다 (중복 방지).
     * 정규화 좌표(vectorMap)와 함께 원본 좌표(rawVectorMap)도 저장한다.
     */
    @Transactional
    public void saveNormalization(HandwritingRequest request) {
        // 로그인한 사용자. userId가 없는 요청(테스트 화면 등)은 1번 사용자로 저장한다.
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        char charName = request.getCharName().charAt(0);

        List<StrokeData> normalization = normalizationService.normalization(request);

        Vowel vowel = vowelRepository
                .findFirstByUser_UserIdAndCharNameOrderByVowelIdDesc(user.getUserId(), charName)
                .orElseGet(Vowel::new);

        vowel.setUser(user);
        vowel.setCharName(charName);
        vowel.setVectorMap(normalization);
        vowel.setRawVectorMap(request.getStrokes());
        vowel.setCharCount(vowel.getCharCount() + 1); // 이 글자를 저장한 횟수

        registerVowel(vowel);
    }
}