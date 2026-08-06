# A방식 구현·배포 작업 과정 및 변경 내역

작성 기준일: 2026-08-04
구현 기준:

- 백엔드 `3886355` (`feat: A 방식 EC2 배포 지원 추가`)
- 프론트엔드 `70fed0f` (`feat: A 방식 배포를 위한 프론트엔드 준비`)
- 운영 검증 기록 `VALIDATION_REPORT.md` (2026-08-04)

이 문서는 A방식인 **단일 EC2 직접 설치 배포**를 구현하면서 진행한 작업,
추가·수정한 코드의 기능과 역할, 실제로 확인된 EC2 환경 및 운영 정보를
정리한다. 위 커밋 이후 추가된 B방식 Docker 변경은 이 문서의 범위가 아니다.

## 1. A방식 개요

A방식은 Ubuntu EC2 한 대에 Java 21, MySQL, Nginx를 직접 설치하고 다음과
같이 운영하는 구조다.

```text
Browser
  -> EC2 Nginx :80
       |- React 정적 파일: /opt/community/frontend/current
       |- /api/*          -> Spring Boot 127.0.0.1:8080
       `- /uploads/*      -> Spring Boot 127.0.0.1:8080
                                  |
                                  `-> MySQL 127.0.0.1:3306
```

- 외부에는 Nginx의 HTTP 80만 노출한다.
- Spring Boot 8080과 MySQL 3306은 EC2 루프백에서만 수신한다.
- 백엔드와 프론트엔드는 버전별 릴리스 디렉터리에 배치하고 심볼릭 링크로
  현재 버전을 가리킨다.
- 운영 데이터는 MySQL과 `/var/lib/community/uploads`에 저장한다.
- Docker, RDS, S3, Elastic IP, ALB, NAT Gateway를 사용하지 않는다.
- AWS 콘솔 작업과 EC2 명령 실행은 운영자가 직접 수행한다.

## 2. 배포·운영 정보

### 2.1 배포 주소

| 항목 | 내용 |
|---|---|
| 배포 방식 | EC2 공개 IPv4의 HTTP 80으로 접속 |
| 주소 형식 | `http://<EC2-PUBLIC-IP>/` |
| 실제 주소 | 저장소와 검증 보고서에 실제 공개 IP 또는 도메인이 기록되어 있지 않아 확인 불가 |
| 접근 범위 | 검증 당시 보안 그룹에서 HTTP 80을 운영자의 `My IP/32`에만 허용 |
| 도메인/TLS | 적용 기록 없음. 443은 외부 검사에서 시간 초과 확인 |
| 주소 안정성 | Elastic IP를 사용하지 않아 EC2 중지/시작 후 공개 IP가 바뀔 수 있음 |

제출 문서에 실제 주소가 필요하면 AWS 콘솔의 현재 `Public IPv4 address`를
확인해 아래 값만 교체해야 한다. 과거 주소를 추측해서 사용하면 안 된다.

```text
배포 주소: http://[현재 EC2 Public IPv4]/
```

현재 구성은 HTTP 검증용이다. 공개 서비스로 전환하려면 도메인과 TLS를
적용한 뒤 `FRONTEND_ORIGIN=https://...`, `COOKIE_SECURE=true`, 보안
그룹 443 허용, Nginx 인증서 설정을 함께 적용해야 한다.

### 2.2 테스트 계정

| 항목 | 내용 |
|---|---|
| 운영 테스트 계정 | 회원가입·로그인 기능 검증은 완료됐지만 계정 ID는 저장소에 기록하지 않음 |
| 운영 테스트 비밀번호 | 기록하지 않음. Git이나 본 문서에 평문으로 추가하면 안 됨 |
| 로컬 H2 시드 | `email@email.com` 계정이 있지만 MySQL 운영 마이그레이션에는 포함되지 않음 |

따라서 `email@email.com`을 운영 테스트 계정이라고 안내하면 안 된다. 운영
테스트 계정이 필요하면 현재 운영 DB에서 사용할 계정을 새로 회원가입하고,
다음 형식으로 계정 소유자에게 **별도 안전 채널**로 전달한다.

```text
테스트 계정 이메일: [운영 환경에서 생성한 이메일]
테스트 계정 비밀번호: [문서/Git에 기록하지 않고 별도 전달]
```

### 2.3 실제 검증된 EC2 환경

| 구분 | 검증된 값 |
|---|---|
| 리전 | 아시아 태평양(서울), `ap-northeast-2` |
| 운영체제 | Ubuntu 서버 24.04 LTS |
| 아키텍처 | x86_64 |
| 인스턴스 유형 | `t3.small` |
| 루트 볼륨 | gp3 20 GiB, 암호화, 인스턴스 종료 시 삭제 |
| 네트워크 | 공개 IPv4 외부 접속 확인, Elastic IP 미사용 |
| 접속 방식 | AWS Systems Manager 세션 관리자 |
| IAM 역할 | `community-ec2-ssm-role` |
| IAM 정책 | `AmazonSSMManagedInstanceCore` |
| 메타데이터 | IMDSv2 필수, 홉 제한 1 |
| 인바운드 | HTTP 80 `My IP/32`만 유지 |
| 외부 차단 확인 | 22, 443, 8080, 3306 시간 초과 |
| 내부 수신 주소 | Nginx 80, Spring Boot `127.0.0.1:8080`, MySQL `127.0.0.1:3306` |
| 설치 실행 환경 | OpenJDK 21 헤드리스, Nginx, MySQL 서버 |
| 서비스 자동 시작 | `mysql`, `nginx`, `community-backend` 활성화 |
| SSM 에이전트 | 활성 확인 |
| 비용 관리 | 월 예산 15 USD, 실제 알림 12 USD를 유지하도록 설계 |

초기 AWS 가이드는 `t3a.small`을 제안했지만 실제 검증 보고서에는
`t3.small`이 기록되어 있다. 이 문서는 계획값보다 실제 검증값을 기준으로
작성했다. Java, Nginx, MySQL의 세부 패치 버전은 검증 기록에 남아 있지
않으므로 임의로 기재하지 않는다.

### 2.4 EC2 파일 시스템과 권한

| 경로 | 소유권/권한 | 역할 |
|---|---|---|
| `/etc/community/backend.env` | `root:community`, `640` | 운영 환경 변수와 비밀값 |
| `/opt/community/backend/releases` | `root:community` | 버전별 백엔드 JAR |
| `/opt/community/backend/app.jar` | 심볼릭 링크 | 현재 백엔드 릴리스 |
| `/opt/community/frontend/releases` | `root:root` | 버전별 프론트엔드 정적 파일 |
| `/opt/community/frontend/current` | 심볼릭 링크 | 현재 프론트엔드 릴리스 |
| `/opt/community/deployment/method-a` | `root:root` | 재부팅 후에도 사용하는 운영 스크립트 |
| `/var/lib/community/uploads` | `community:community`, 디렉터리 `750` | 업로드 이미지 영속 저장소 |
| `/var/lib/community/backup` | 루트 관리 | DB·업로드 백업 |

Spring Boot는 로그인할 수 없는 전용 시스템 사용자 `community`로 실행한다.

## 3. 환경 변수

실제 값은 `/etc/community/backend.env`에만 저장한다. 스크립트는 값을 화면에
출력하지 않고, 검증 스크립트도 `SET` 또는 `MISSING`만 표시한다.

| 변수 | 예시 또는 고정값 | 기능과 역할 | 민감도 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `aws` | AWS 운영 프로파일을 활성화 | 일반 |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/community?...` | 루프백 MySQL의 `community` DB 연결 주소 | 제한적 공개 |
| `DB_USERNAME` | `community_app` | 애플리케이션 최소 권한 DB 사용자 | 제한적 공개 |
| `DB_PASSWORD` | 실제 값 비공개 | MySQL 애플리케이션 사용자 인증 | 비밀값 |
| `JWT_SECRET` | 실제 값 비공개 | 접근/갱신 토큰 서명. Base64 디코딩 결과가 32바이트 이상이어야 함 | 비밀값 |
| `FRONTEND_ORIGIN` | `http://<현재 Public IP>` | CORS/CSRF에서 허용할 정확한 출처 | 환경별 값 |
| `COOKIE_SECURE` | HTTP 검증 `false`, HTTPS 운영 `true` | HTTPS에서만 인증 쿠키를 전송하도록 제어 | 일반 |
| `DB_POOL_MAX_SIZE` | `10` | HikariCP 최대 DB 연결 수 | 일반 |
| `DB_POOL_MIN_IDLE` | `2` | HikariCP 최소 유휴 연결 수 | 일반 |

`FRONTEND_ORIGIN`은 스킴, 호스트, 선택적 포트만 허용한다. 마지막 `/`, 경로,
쿼리와 프래그먼트는 허용하지 않는다. `JWT_SECRET`의 Base64는 암호화가 아니므로
값 자체를 노출하면 안 된다.

## 4. 작업 과정

### 4.1 기존 구조와 위험 요소 분석

1. 백엔드의 기본 H2 설정, Flyway 마이그레이션, JWT 설정, 이미지 저장 방식을
   확인했다.
2. 프론트엔드가 현재 호스트의 8080으로 직접 API를 호출하고 있어 EC2의 8080을
   외부에 열지 않으면 운영에서 동작하지 않는 문제를 확인했다.
3. H2 전용 SQL을 MySQL에 그대로 적용할 수 없는 타입·문법 차이를 분리했다.
4. 업로드 파일의 MIME 위조, 경로 이탈, 실제 크기 초과 위험을 확인했다.
5. 비밀값, 비공개 키, 백업이 Git에 들어가지 않도록 제외 범위를 정했다.

### 4.2 AWS 보안·비용 기반 준비

1. 서울 리전에서 예산 15 USD와 실제 알림 12 USD 기준을 정했다.
2. 세션 관리자 전용 IAM 역할과 최소 관리형 정책을 구성했다.
3. 보안 그룹은 초기 인바운드 없이 만들고, 배포 검증 때만 HTTP 80을
   `My IP/32`로 허용했다.
4. Ubuntu 24.04 x86_64, 루트 gp3 20 GiB 암호화, IMDSv2 필수 조건으로
   EC2를 준비했다.
5. 22, 8080, 3306을 외부에 공개하지 않는 것을 완료 조건으로 정했다.

### 4.3 운영 프로파일과 MySQL 마이그레이션 분리

1. 공통 설정은 `application.yaml`, 로컬 H2는 `application-local.yaml`, 운영
   MySQL은 `application-aws.yaml`로 분리했다.
2. H2 마이그레이션을 `db/migration/h2`로 이동했다.
3. MySQL 전용 `db/migration/mysql/V1__create_initial_schema.sql`을 추가했다.
4. 운영은 Flyway로 스키마를 만들고 Hibernate `ddl-auto=validate`로 엔터티와
   DB 스키마의 불일치만 검사하도록 했다.
5. H2 콘솔은 로컬 프로파일에서만 공개하고 AWS 프로파일에서는 비활성화했다.

### 4.4 백엔드 운영 안전성 보강

1. 이미지 저장 추상화 `ImageStorageService`를 추가해 컨트롤러가 구현체에
   직접 의존하지 않도록 변경했다.
2. 파일 확장자만 믿지 않고 PNG/JPEG/WebP 매직 바이트를 검증했다.
3. 업로드 스트림을 읽으면서 실제 10 MiB 제한을 적용하고 실패 시 일부 저장된
   파일을 제거했다.
4. 원본 파일명 대신 UUID 파일명을 사용하고 실제 경로가 저장 루트 하위인지
   검사했다.
5. 게시글·댓글 작성 URL의 사용자 ID를 `/me`로 바꿔 인증 토큰의 사용자와
   URL 사용자 ID가 불일치할 여지를 없앴다.
6. 게시글과 댓글 조회에 `EntityGraph`를 적용해 `open-in-view=false` 환경에서
   작성자 지연 로딩 오류와 N+1 가능성을 줄였다.

### 4.5 프론트엔드 운영 연결 방식 수정

1. 로컬 호스트에서는 개발 백엔드 `:8080`을 사용하도록 유지했다.
2. EC2 운영 호스트에서는 API 기본 URL을 빈 문자열로 만들어 `/api`, `/uploads`
   요청이 Nginx 동일 출처 방식으로 들어가게 했다.
3. 게시글·댓글 API에서 사용자 ID 인자를 제거하고 백엔드의 `/me` 계약에
   맞췄다.
4. 같은 커밋에 포함된 피드 소유자 옵션 메뉴, 북마크, 메뉴 위치·접근성 동작을
   함께 수정하고 관련 테스트를 보강했다.

### 4.6 산출물 생성과 전송

백엔드 저장소에서 다음 과정을 수행하도록 구성했다.

```bash
./gradlew clean test bootJar
```

프론트엔드 저장소에서 다음 과정을 수행하도록 구성했다.

```bash
npm run format:check
npm run test:unit
npm run test:integration
npm run build:react
```

생성한 백엔드 JAR, `index.html`과 `dist`를 묶은 프론트엔드 압축 파일, A방식 운영
묶음을 EC2로 전송했다. S3를 사용하지 않았기 때문에 산출물 전송 시에만
SSH 22를 현재 IP `/32`로 임시 허용하고, 전송 직후 규칙과 임시 공개키를
삭제했다.

### 4.7 EC2 초기 설치

1. `01-install-packages.sh`로 Java 21, Nginx, MySQL, 운영 도구를 설치했다.
2. `02-configure-user-and-directories.sh`로 전용 사용자와 최소 권한 경로를
   만들었다.
3. `03-configure-mysql.sh`로 루프백 MySQL, `community` DB,
   `community_app` 사용자를 구성했다.
4. `configure-backend-env.sh`로 비밀값을 화면에 출력하지 않고 환경 파일을
   생성했다.
5. `install-operations.sh`로 운영 스크립트를 `/opt/community/deployment/method-a`
   에 영구 설치했다.

### 4.8 릴리스 배포

`deploy.sh`가 다음 순서로 배포를 조정한다.

1. 백엔드 JAR을 UTC 타임스탬프가 붙은 릴리스 파일로 설치한다.
2. 프론트엔드 압축 파일의 절대 경로·`..` 경로를 차단한 뒤 새 릴리스 디렉터리에
   압축 해제한다.
3. 백엔드 `app.jar`와 프론트엔드 `current` 심볼릭 링크를 새 릴리스로 교체한다.
4. 강화된 systemd 단위와 Nginx 설정을 설치한다.
5. Flyway 실행에 필요한 DDL 권한만 일시적으로 DB 사용자에게 부여한다.
6. 백엔드와 Nginx를 재시작하고 `/api/csrf`가 최대 60초 안에 응답하는지
   확인한다.
7. 성공·실패와 관계없이 임시 DDL 권한을 회수한다.

### 4.9 검증·롤백·백업·복구

1. `verify.sh`로 서비스 상태, Nginx 문법, 8080/3306 루프백, 상태 확인 URL,
   H2 콘솔 차단, 환경 파일 권한, 필수 변수 존재, 업로드 권한을 검사했다.
2. 브라우저에서 회원가입, 로그인, 로그아웃, 게시글, 댓글, 좋아요, 북마크,
   PNG/JPEG/WebP 업로드와 재시작 후 영속성을 확인했다.
3. 이전 백엔드와 프론트엔드 릴리스로 동시에 전환하는 롤백을 확인했다.
4. `mysqldump`와 업로드 압축 파일을 SHA-256과 함께 백업하고 복원을
   검증했다.
5. EC2 재부팅 후 세 서비스의 자동 시작과 데이터 영속성을 다시 확인했다.

## 5. 백엔드 코드 변경 상세

### 5.1 빌드와 설정

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `.gitignore` | AWS 키, PEM, `.env`, 운영 환경 파일, 업로드, 백업, 덤프, 비공개 증빙 제외 | 비밀값과 개인정보가 Git에 들어가는 것을 방지 |
| `build.gradle` | MySQL Connector/J, Flyway MySQL 실행 환경 추가 | AWS 프로파일에서 MySQL 접속과 MySQL 마이그레이션 실행 |
| `application.yaml` | 기본 프로파일을 `local`로 지정하고 공통 JPA/JWT 설정만 유지 | 환경별 설정 중복과 운영 설정 혼입 방지 |
| `application-local.yaml` | H2 메모리 DB, H2 콘솔, Hibernate 업데이트, Flyway 비활성 | 로컬 개발 편의 유지 |
| `application-aws.yaml` | 루프백 8080, MySQL/Hikari, Flyway, JPA 검증, H2 차단, 업로드·쿠키 설정 | EC2 운영 전용 안전 설정 |

A방식 구현 시점의 `application-aws.yaml`은 `server.address=127.0.0.1`을
고정했다. 현재 작업 브랜치에 보이는 `SERVER_ADDRESS`, Actuator 상태 확인,
정상 종료 설정은 이후 B방식 커밋에서 추가된 내용이다.

### 5.2 DB 마이그레이션

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `db/migration/h2/V1~V5` | 기존 H2 SQL을 전용 디렉터리로 이동 | H2 문법과 MySQL 문법의 혼용 방지 |
| `db/migration/mysql/V1__create_initial_schema.sql` | 테이블 9개, FK, 고유 제약 조건, 인덱스 생성 | MySQL 8 계열의 운영 초기 스키마 생성 |

MySQL 마이그레이션은 `users`, `posts`, `post_comments`, `post_images`,
`post_likes`, `post_views`, `refresh_tokens`, `bookmarks`,
`token_family_sessions`를 생성한다. `InnoDB`, `utf8mb4`,
`utf8mb4_0900_ai_ci`, `DATETIME(6)`, `AUTO_INCREMENT`를 명시하고 로컬 시드는
포함하지 않는다.

### 5.3 이미지 저장

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `ImageStorageService.java` | `storeProfile`, `storeFeed` 인터페이스 추가 | 컨트롤러와 저장 구현체 분리 |
| `LocalImageStorageService.java` | MIME·매직 바이트·실제 크기·실체 경로 검증, UUID 이름, 실패 파일 삭제 | 악성·위조 업로드와 경로 이탈 차단 |
| `UserController.java` | 구체 클래스 대신 인터페이스 주입 | 사용자 프로필 이미지 저장 구현 교체 가능성 확보 |
| `PostController.java` | 구체 클래스 대신 인터페이스 주입 | 피드 이미지 저장 구현 교체 가능성 확보 |

지원 형식은 PNG, JPEG, WebP이고 실제 스트림 기준 최대 크기는 10 MiB다.

### 5.4 인증·API·조회 안정성

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `SecurityConfig.java` | `spring.h2.console.enabled`일 때만 H2 CSRF 예외와 공개 경로 적용 | AWS에서 H2 콘솔이 인증 없이 노출되는 문제 차단 |
| `PostController.java` | 게시글 작성과 댓글 작성·수정·삭제 경로를 사용자 ID 대신 `/me`로 변경 | URL 조작 대신 인증 주체를 신뢰하도록 계약 정리 |
| `PostRepository.java` | 게시글·작성자 `EntityGraph` 추가 | 트랜잭션 밖 응답 변환 시 지연 로딩 실패 방지 |
| `CommentRepository.java` | 댓글의 작성자·게시글 `EntityGraph` 추가 | 상세 조회 안정화와 추가 쿼리 감소 |

### 5.5 백엔드 테스트

| 파일 | 검증 역할 |
|---|---|
| `AwsDeploymentConfigurationTest.java` | AWS가 루프백·MySQL·JPA 검증·MySQL 마이그레이션·H2 차단을 사용하는지 확인 |
| `LocalDatabaseConfigurationTest.java` | 로컬 환경이 H2 메모리·Hibernate 업데이트·Flyway 비활성을 유지하는지 확인 |
| `MigrationIntegrationTest.java` | H2 마이그레이션 위치를 명시하고 마이그레이션 유효성 확인 |
| `LocalImageStorageServiceTest.java` | UUID 저장, 위조 MIME 거부, 10 MiB 초과 거부, 콘텐츠 유형 누락 거부 확인 |
| `PostDetailApiIntegrationTest.java` | `/me` 게시글 작성, 타인 댓글 포함 상세 조회, 댓글 작성 후 상세 재조회 확인 |
| `PostRepositoryTest.java` | 피드 조회 시 작성자 엔터티가 함께 초기화되는지 확인 |
| `SecurityCsrfTest.java` | H2 비활성 프로파일에서 미인증 접근이 거부되는지 확인 |
| `application-test.yml` | 테스트 프로필 활성 조건 명시 |

## 6. 프론트엔드 코드 변경 상세

### 6.1 A방식 배포에 직접 필요한 변경

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `src/shared/config/env.js` | 로컬 호스트만 `:8080`, 그 외 호스트는 빈 API 기본 URL 사용 | 운영에서 Nginx 동일 출처 `/api`, `/uploads` 사용 |
| `src/entities/post/api/postApi.js` | 사용자 ID 인자 제거, 게시글·댓글 경로를 `/me`로 변경 | 백엔드 인증 주체 기반 API와 계약 일치 |
| `CommentForm.jsx` | `userId` 속성 제거 | 댓글 작성 시 조작 가능한 사용자 ID 전송 제거 |
| `EditCommentModal.jsx` | `userId` 속성 제거 | 댓글 수정 API 계약 일치 |
| `DeleteCommentModal.jsx` | `userId` 속성 제거 | 댓글 삭제 API 계약 일치 |
| `CreatePostModal.jsx` | 게시글 생성 시 `user.userId` 전달 제거 | 게시글 작성 API 계약 일치 |
| `PostDetailPage.jsx` | 하위 댓글 컴포넌트에 `userId` 전달 제거 | 변경된 API 호출 흐름 연결 |
| 프론트엔드 `.gitignore` | 키, `.env`, 덤프, 백업, 압축 파일 제외 | 프론트엔드 저장소의 비밀값 유입 방지 |

### 6.2 같은 커밋에 포함된 기능·UI 변경

아래 변경은 A방식의 Nginx 연결 자체에 필수는 아니지만 동일 커밋에 포함되어
운영 산출물에 반영됐다.

| 파일 | 변경 내용 | 기능과 역할 |
|---|---|---|
| `PostCard.jsx` | 소유자 옵션을 푸터에 표시하고 메뉴 상태·북마크를 통합 | 본인 피드의 저장·수정·삭제 동작 제공 |
| `OptionMenu.jsx` | 북마크 항목, 위/아래 자동 배치, 외부 클릭·ESC 닫기, 포커스 복원 | 좁은 뷰포트와 키보드 접근성 개선 |
| `FeedPage.jsx` | 소유자 판별을 한 번 수행하고 옵션 배치 전달, 불필요한 마지막 메시지 제거 | 피드 렌더링과 소유자 메뉴 흐름 단순화 |
| `PostDetailPage.jsx` | 상세 화면에도 소유자 푸터 메뉴 적용 | 피드와 상세의 메뉴 동작 통일 |
| `app.css` | 카드 경계, 메뉴 Z축 순서·위치, 동작 간격, 반응형 스타일 수정 | 새 옵션 메뉴가 잘리지 않고 일관되게 표시되도록 지원 |
| `dist/app.js`, `dist/app.css` | 변경된 React/CSS 운영 번들 반영 | EC2에 실제 배포되는 정적 결과물 |
| `styles/tailwind-output.css` | 사용 클래스 반영 | 정적 스타일 결과 갱신 |

단위, 통합, Playwright UI, 백엔드 연동 E2E 테스트의 API 기대 경로와
소유자 메뉴·북마크·포커스·뷰포트 시나리오도 함께 수정하거나 추가했다.

## 7. 배포 운영 파일의 기능과 역할

### 7.1 설정과 문서

| 파일 | 기능과 역할 |
|---|---|
| `deployment/method-a/README.md` | A방식 아키텍처, 책임 범위, 파일 진입점 설명 |
| `docs/AWS_CONSOLE_GUIDE.md` | 예산, IAM, 보안 그룹, EC2 생성 절차와 중단 기준 |
| `docs/EC2_MANUAL_GUIDE.md` | 산출물 생성부터 재부팅 검증까지 운영 명령 설명 |
| `docs/DB_COMPATIBILITY.md` | H2/MySQL의 시간, 불리언, 정렬 규칙, 페이지네이션, 스키마 차이 기록 |
| `docs/VALIDATION_REPORT.md` | 로컬·보안·AWS·비용의 통과/경고 증빙 기록 |
| `env/backend.env.example` | 운영 환경 변수 이름과 형식 제공. 실제 비밀값은 포함하지 않음 |
| `mysql/community.cnf.example` | MySQL 루프백, utf8mb4, 연결·버퍼·느린 쿼리 설정 |
| `nginx/community.conf` | SPA 정적 제공, `/api`·`/uploads` 프록시, 상태 확인 URL, 보안 헤더, 민감 확장자 차단 |
| `systemd/community-backend.service` | 전용 사용자, JVM 힙, 자동 재시작, 파일 시스템·커널 보호, 업로드만 쓰기 허용 |

### 7.2 설치·배포 스크립트

| 스크립트 | 기능과 역할 |
|---|---|
| `lib/common.sh` | 루트·명령·파일 검사, 안전 경로, 출처/JWT 검증, 타임스탬프, 루프백 판정 공통 함수 |
| `01-install-packages.sh` | Java 21, MySQL, Nginx, cURL, rsync, tar 등 설치 및 서비스 활성화 |
| `02-configure-user-and-directories.sh` | `community` 사용자·그룹, 릴리스·업로드·백업·환경 파일 경로와 권한 생성 |
| `03-configure-mysql.sh` | MySQL 루프백 설정, DB 생성, 비밀번호 숨김 입력, DML 최소 권한 사용자 생성 |
| `configure-backend-env.sh` | 출처·DB 비밀번호·JWT 입력 검증 후 `root:community 640` 환경 파일 생성 |
| `install-operations.sh` | 스크립트 묶음을 운영 경로에 루트 소유로 동기화 |
| `04-deploy-backend.sh` | JAR 유효성 검사, 타임스탬프 릴리스 설치, `app.jar` 링크 교체 |
| `05-deploy-frontend.sh` | 압축 파일 경로 이탈 검사, 필수 파일 검사, 권한 정리, `current` 링크 교체 |
| `06-configure-systemd.sh` | 환경 파일 권한 확인 후 강화된 백엔드 단위 설치·활성화 |
| `07-configure-nginx.sh` | 사이트 설정 설치, 기본 사이트 제거, 문법 검사, 다시 불러오기 |
| `mysql-migration-access.sh` | 배포 중에만 Flyway DDL 권한 부여, 완료 후 회수 |
| `deploy.sh` | 전체 릴리스 배포, 서비스 재시작, 준비 상태, 권한 회수를 조정 |
| `verify.sh` | 서비스·포트·H2·상태 확인·환경 파일·업로드 권한 종합 검사 |
| `rollback.sh` | 이전 백엔드와 프론트엔드 릴리스를 함께 선택해 전환하고 실패 시 원복 |
| `backup.sh` | MySQL 덤프와 업로드를 묶고 SHA-256을 생성해 `600` 압축 파일로 저장 |
| `restore.sh` | 승인 경로·확인 문자열·압축 파일 경로·체크섬을 검증한 뒤 DB와 업로드 복구 |
| `tests/common-functions-test.sh` | 출처, JWT, 경로, 루프백 판정 함수의 정상·경계·실패 사례 검증 |

모든 스크립트는 `set -Eeuo pipefail`을 사용하며 비밀값 노출을 막기 위해
`set -x`를 사용하지 않는다.

## 8. 검증 결과

2026-08-03 검증 보고서 기준 결과는 다음과 같다.

| 구분 | 결과 |
|---|---|
| 백엔드 테스트 | 53개 통과, 실패/오류/건너뜀 0 |
| 백엔드 JAR | `clean test bootJar` 통과 |
| 프론트엔드 형식 | 통과 |
| 프론트엔드 단위 | 125개 통과 |
| 프론트엔드 통합 | 19개 통과 |
| 프론트엔드 UI Playwright | 105개 통과 |
| 프론트엔드–백엔드 E2E | 12개 통과 |
| 프론트엔드 운영 빌드 | 통과, 대용량 자산 경고 3건 |
| 셸 문법 | 모든 스크립트 `bash -n` 통과 |
| 배포 공통 함수 | 정상·경계값 통과 |
| 소스 맵 | 운영 묶음에 생성되지 않음 |
| 운영 서비스 | MySQL, Spring Boot, Nginx 활성 |
| 기능 검증 | 로그인·피드·업로드·재시작 영속성 통과 |
| 롤백 | 두 릴리스 간 전환과 최신 복귀 통과 |
| 백업/복원 | SHA-256 포함 통과 |
| 최종 백업 | `community-20260803T053936Z.tar.gz`, `root:root 600` |

ShellCheck는 로컬에 명령이 설치되어 있지 않아 실행하지 못했고 경고로
기록됐다. 이는 통과로 바꾸어 적으면 안 된다.

## 9. 운영 시 주의 사항

- 실제 배포 주소는 AWS 콘솔의 현재 공개 IPv4로 확인한다.
- 실제 테스트 계정과 비밀번호는 Git 또는 이 문서에 추가하지 않는다.
- HTTP 검증 상태에서는 보안 쿠키가 비활성화되므로 공개 운영 전에 TLS를
  적용한다.
- 8080과 3306을 보안 그룹에 추가하지 않는다.
- SSH 22는 산출물 전송이 불가피할 때만 현재 IP `/32`로 열고 즉시 삭제한다.
- 운영 DB에 H2 시드를 적용하지 않는다.
- 적용된 Flyway 마이그레이션 파일을 수정하지 말고 새 버전 마이그레이션을
  추가한다.
- 백업은 개인정보를 포함할 수 있으므로 Git이나 증빙에 첨부하지 않는다.
- 중지/시작 뒤 공개 IP와 `FRONTEND_ORIGIN`의 일치 여부를 다시 확인한다.
- 비용 경보는 자동 지출 차단이 아니므로 사용하지 않을 때 EC2를 직접 중지한다.

## 10. 제출 전 채워야 할 값

현재 저장소만으로 확인할 수 없는 다음 두 값은 제출 직전에 운영자가
확인해야 한다.

```text
배포 주소: https://pulse.gleeze.com/
테스트 계정 이메일: [운영 환경에서 생성한 계정]
테스트 계정 비밀번호: [별도 안전 채널 전달]
```

비밀번호를 이 문서에 직접 채우지 않는다. 제출 양식상 비밀번호 기재가
반드시 필요하다면 저장소 밖의 접근 제한된 전달 수단을 사용한다.

## 11. 무료 고정 도메인과 HTTPS 추가 (2026-08-04)

위 1~10장은 `3886355` 기준의 HTTP 검증 이력을 보존한다. 2026-08-04에는
다음 공개 운영 구성을 실제 EC2에 추가하고 최종 검증했다.

- Dynu `pulse` 무료 고정 호스트 이름과 10분 주기 공개 IPv4 자동 갱신
- 계정 비밀번호와 분리된 IP 업데이트 비밀번호 사용 및 SHA-256만 저장
- Let's Encrypt HTTP-01 인증서 발급과 Certbot 자동 갱신 모의 실행
- Nginx 80 → 표준 HTTPS 주소 리디렉션, 443 TLS 1.2/1.3, HSTS
- 백엔드 `FRONTEND_ORIGIN=https://...` 자동 전환과 보안 쿠키 활성화
- 도메인·토큰·타이머·인증서·리디렉션·HTTPS를 검사하는 `verify.sh` 통제 항목

검증된 주소는 `https://pulse.gleeze.com/`이다. Dynu API 갱신, Let's Encrypt
인증서 발급, 자동 갱신 모의 실행, HTTP 301 리디렉션, HTTPS 상태 확인 200,
`community-dynu.timer`와 `certbot.timer`, 보안 쿠키 전환 및 최종
`verify.sh` 전체 통과를 확인했다. IP 업데이트 비밀번호와 해시는 저장소에
기록하지 않는다.
