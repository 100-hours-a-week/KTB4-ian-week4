# B 방식 빌드 및 배포 워크플로

## 로컬 기준선

백엔드는 Java 21과 Gradle 래퍼 9.5.1이 필요합니다. `aws` 프로필은 A 방식의 기본
수신 주소인 `127.0.0.1`을 유지하고, Compose는 비공개 컨테이너 네트워크 안에서만 이를
`0.0.0.0`으로 명시적으로 재정의합니다. 로컬 개발은 변경되지 않은 H2 프로필을 계속
사용합니다.

이미지를 빌드하기 전에 소스 검사를 실행합니다.

```bash
./gradlew clean test bootJar
```

결정적으로 생성되는 부팅 산출물은 `build/libs/community.jar`입니다.

## linux/amd64 이미지 빌드

백엔드 저장소에서 실행합니다.

```bash
IMAGE_TAG=community-backend:<backend-commit> \
  ./deployment/ec2-compose/scripts/build-backend-image.sh
```

프론트엔드 저장소에서 실행합니다.

```bash
IMAGE_TAG=community-frontend:<frontend-commit> \
  ./scripts/build-image.sh
IMAGE_TAG=community-frontend:<frontend-commit> \
  ./scripts/verify-image.sh
```

`compose.example.env`에서 로컬 릴리스 환경 파일을 만들고 위의 정확한 태그를 사용합니다.
오프라인 전송을 위해 프론트엔드, 백엔드 및 고정된 MySQL 이미지를 패키징합니다.

```bash
RELEASE_ENV=/absolute/path/to/release.env \
OUTPUT_DIR=/absolute/path/to/private/artifacts \
  ./deployment/ec2-compose/scripts/package-images.sh
```

출력물은 tar 파일 3개와 `SHA256SUMS`로 구성됩니다. 압축 파일에는 독점 애플리케이션
코드가 포함될 수 있으므로 Git에 추가하지 않습니다.

## 로컬 Compose 통합 테스트

권한이 제한된 임시 비운영 비밀 파일을 사용하고, 비공개 릴리스 환경 파일에서 바인드
주소와 데이터 루트를 재정의합니다.

```text
HTTP_BIND_ADDRESS=127.0.0.1
HTTP_PORT=8088
DATA_ROOT=<private-absolute-test-directory>
SECRETS_DIR=<private-absolute-secret-directory>
FRONTEND_ORIGIN=http://127.0.0.1:8088
COOKIE_SECURE=false
```

그런 다음 설정을 검증하고 시작합니다.

```bash
docker compose --env-file <release-env> \
  --file deployment/ec2-compose/compose.yaml config --quiet
docker compose --env-file <release-env> \
  --file deployment/ec2-compose/compose.yaml up --detach --wait
```

SPA 새로 고침, 인증, 게시글, 댓글, 북마크, 이미지 업로드, 로그아웃, 잘못된 입력,
한국어·이모지 콘텐츠, 브라우저 콘솔·네트워크 오류, 재시작, `down`/`up` 및 데이터
보존을 검증합니다. `docker compose port backend 8080`과
`docker compose port mysql 3306`이 매핑을 반환하지 않는지 확인합니다. `down -v`는
사용하지 않습니다.

## 초기 자원 범위

Compose 파일의 초기 제한은 Nginx 128 MiB, 백엔드 900 MiB, MySQL 640 MiB입니다.
백엔드 힙은 256~640 MiB이고 MySQL 버퍼 풀은 320 MiB입니다. 이는 시작 제약이며 성능에
대한 결론이 아닙니다. EC2에서 다음을 검토합니다.

```bash
free -h
docker stats --no-stream
docker inspect --format '{{.RestartCount}}' <container-id>
journalctl -k --grep='Out of memory\|Killed process'
```

호스트 여유 메모리, 재시작 횟수, 메모리 부족 사건, JVM 힙, MySQL 메모리 및 P95 응답
시간을 기록한 후에만 조정합니다.
