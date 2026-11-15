# 손글씨 폰트 생성 서비스 — 백엔드 (Spring Boot)

> 졸업작품 「자모 단위 필기 획 합성 기반 개인 맞춤형 한글 폰트 생성 응용 프로그램 구현」의 백엔드 서버

사용자가 태블릿에서 자음 14자·모음 12자, 총 26자를 손으로 쓰면 현대 한글 11,172자를 담은 TTF 폰트를 만들어 주는 서비스입니다.
이 저장소는 회원 관리, 자모 검증·저장, 폰트 생성 요청과 파일 관리를 맡는 REST API 서버입니다.

## 관련 저장소

| 구성 | 역할 |
| --- | --- |
| 프론트엔드 (Flutter 웹) | 손글씨 캔버스, 회원 화면, 폰트 만들기·다운로드 화면 |
| **백엔드 (Spring Boot)** | 회원·자모·폰트 관리 REST API (이 저장소) |
| AI·폰트 서버 (FastAPI) | CNN 자모 검증, 11,172자 음절 배치, TTF 생성 |

## 기술 스택

- Java 21, Spring Boot 4
- Spring Data JPA, MySQL 8.4
- Spring Mail (Gmail SMTP) — 회원가입 이메일 인증
- spring-security-crypto (BCrypt) — 비밀번호 암호화
- RestClient — FastAPI 서버 호출

## 주요 기능

- **회원가입·로그인·탈퇴**: 6자리 이메일 인증번호(5분 유효, 60초 재전송 제한), BCrypt 비밀번호 저장, 탈퇴 시 폰트 파일과 모든 데이터 삭제
- **자모 검증**: 획 좌표를 FastAPI `/predict`로 보내, 예측 자모가 쓰려던 자모와 같고 코사인 유사도가 0.6 이상일 때만 통과
- **자모 저장**: 26자가 모두 모였는지 확인한 뒤 하나의 트랜잭션으로 원본 좌표와 정규화 좌표(0~1)를 저장
- **폰트 생성**: 저장된 자모 26자를 FastAPI `/make-font-from-jamo`로 보내 TTF를 받고, `fonts/{회원 번호}/{생성 시각}_{폰트 이름}.ttf`로 저장
- **폰트 조회·다운로드**: 내 폰트 목록 조회, 소유자 확인 후 TTF 파일 전송

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/user/email/send` | 인증번호 메일 발송 |
| POST | `/api/user/email/verify` | 인증번호 확인 |
| POST | `/api/user/signup` | 회원가입 |
| POST | `/api/user/login` | 로그인 |
| POST | `/api/user/withdraw` | 회원 탈퇴 |
| POST | `/api/handwriting/verify` | 자모 한 자 검증 |
| POST | `/api/handwriting/jamo/save-all` | 자모 26자 일괄 저장 |
| GET | `/api/handwriting/jamo` | 저장된 자모 조회 |
| POST | `/api/font/make` | 폰트 생성 |
| GET | `/api/font` | 내 폰트 목록 |
| GET | `/api/font/{fontId}/file` | TTF 파일 다운로드 |

## 데이터베이스

| 테이블 | 내용 |
| --- | --- |
| `user` | 아이디, 이메일, 이름, BCrypt 비밀번호 |
| `consonant` | 회원별 자음 14자의 원본·정규화 좌표 (JSON) |
| `vowel` | 회원별 모음 12자의 원본·정규화 좌표 (JSON) |
| `font` | 폰트 이름, 획 두께, 글리프 수, 파일 경로, 생성 시각 |

`user` 1 : N `consonant` / `vowel` / `font` 관계입니다. 쌍자음·겹받침·합성 모음 같은 파생 자모는 저장하지 않고 폰트를 만들 때 합성합니다.

## 실행 방법

### 1. 준비

- JDK 21, MySQL 8.4
- FastAPI 서버가 `8000`번 포트에서 실행 중이어야 합니다.

### 2. 설정 파일

`src/main/resources/application.properties`는 비밀번호가 들어가므로 저장소에 올리지 않습니다. 아래 내용으로 직접 만들어 주세요.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/{DB 이름}
spring.datasource.username={DB 사용자}
spring.datasource.password={DB 비밀번호}
spring.jpa.hibernate.ddl-auto=update

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username={Gmail 주소}
spring.mail.password={Gmail 앱 비밀번호}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

fastapi.base-url=http://localhost:8000
handwriting.similarity-threshold=0.6
font.storage-dir=fonts
```

### 3. 실행

IntelliJ IDEA에서 `HandwritingApplication`을 실행하거나, 터미널에서 실행합니다.

```bash
./gradlew bootRun     # Gradle
./mvnw spring-boot:run  # Maven
```

서버는 `http://localhost:8080`에서 실행됩니다.

## 주의

- `application.properties`와 생성된 폰트 폴더 `fonts/`는 `.gitignore`에 포함되어 있습니다.
