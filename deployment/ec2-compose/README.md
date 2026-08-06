# EC2 Docker Compose 배포(B 방식)

재현 가능한 로컬 Compose 런타임 검증 결과는
[`VALIDATION_REPORT.md`](./VALIDATION_REPORT.md)에 기록합니다.

A/B 방식의 배경과 기존 운영 검증 기록은
[`../DEPLOYMENT_TECH_BLOG.md`](../DEPLOYMENT_TECH_BLOG.md)를 참고합니다.

이 디렉터리는 Ubuntu 24.04 `linux/amd64` EC2 인스턴스 하나에서 정확히 4개 서비스를
운영합니다. 엣지 Nginx만 호스트 포트 80과 443을 공개합니다. MySQL, 백엔드 및
프론트엔드 정적 원본은 Compose 브리지 네트워크에서 서비스 이름으로만 접근할 수
있습니다. TLS는 엣지 Nginx에서 종료합니다.

```text
host :80/:443 -> edge nginx:8080/8443
                    |-- /                  -> frontend:8080
                    |-- /api,/uploads,...  -> backend:8080 -> mysql:3306
                                                   |       -> /data/community/mysql
                                                   `--------> /data/community/uploads
```

AWS 콘솔과 모든 EC2 명령은 운영자가 직접 관리합니다. 스크립트를 검토한 후에만
실행합니다. 도움을 요청할 때 비밀값, 전체 `docker inspect` 출력, 데이터베이스 덤프
또는 비공개 증빙을 제공하지 않습니다.

## EC2 파일 시스템 구성

이 디렉터리를 `/opt/community/deployment/ec2-compose`에 배치합니다. 실행 데이터는
체크아웃 외부에 보관합니다.

```text
/data/community/mysql
/data/community/uploads
/data/community/backup
/data/community/evidence
/data/community/releases
/data/community/acme
/etc/community/secrets
/etc/community/tls
/opt/community/configs/<backend-config-sha>
```

## 일회성 호스트 준비

세션 관리자 셸에서 실행합니다.

```bash
cd /opt/community/deployment/ec2-compose
sudo ./scripts/install-docker.sh
sudo ./scripts/prepare-directories.sh
```

예상 결과: Docker와 Compose 버전이 출력되고 데이터 디렉터리가 존재하며 비어 있는 비밀
파일 3개가 보고됩니다. `secrets/README.md`의 설명에 따라 `sudoedit`으로 파일 값을
입력합니다. 비밀값을 명령 인수로 전달하지 않습니다.

## 릴리스 입력값과 관문

[`GATES.md`](./GATES.md)를 따릅니다. 보호된 백엔드 워크플로를 통한 레지스트리 배포가
기본 경로입니다. 이 경로는 신뢰하는 정확한 백엔드 설정 SHA를 준비하고, 변경 불가능한
이미지·커밋 쌍을 검증하고, 격리된 전체 스택 스모크 검사를 실행한 뒤 검사에 통과한
경우에만 `deploy.sh`를 호출합니다.

아래 tar 워크플로는 운영자가 실행하는 오프라인 대체 경로로만 사용합니다.
`compose.offline.yaml`을 사용하고 자동 레지스트리 경로와 혼합하지 않습니다.

`compose.example.env`를 `.env`로 복사하고 모든 꺾쇠괄호 자리표시자를 교체한 뒤 파일
모드를 `0600`으로 유지합니다. 이 파일에는 이미지 태그와 공개 설정만 있으며 비밀값은
없습니다. `manifests/release-manifest.example`을 `release-manifest`로 복사하고 실제
커밋과 UTC 배포 시각을 기록합니다.

이미지 tar 파일과 `SHA256SUMS`를 비공개 디렉터리로 전송한 다음 실행합니다.

```bash
sudo ARTIFACT_DIR=/opt/community/artifacts/<release-id> \
  ./scripts/load-images.sh
```

예상 결과: 모든 체크섬이 `OK`로 표시된 다음 Docker가 불러온 이미지 태그가 출력됩니다.
체크섬 실패는 즉시 중단 조건입니다.

## 배포 및 검증

```bash
sudo ./scripts/deploy.sh
sudo ./scripts/verify.sh
sudo docker compose --env-file /data/community/releases/current.env \
  --file compose.yaml ps
sudo docker stats --no-stream
```

배포는 4개 서비스가 모두 정상 상태가 되고 엣지·API·데이터베이스 검증을 통과한 뒤에만
릴리스 상태를 승격합니다. 검증 항목은 아키텍처, 상태, 루트가 아닌 사용자, 읽기 전용
애플리케이션·엣지 파일 시스템, 호스트 포트 공개 범위, 차단된 Actuator·H2·설정 경로,
비밀값 메타데이터, 캐시와 본문 크기 제한 정책, Flyway·MySQL 연결 및 데이터베이스·업로드
영속성입니다. 보안 그룹, EBS 암호화, IMDSv2 또는 AWS 비용 설정은 검증할 수 없습니다.

시작에 실패하면 환경이나 검사 데이터를 덤프하지 말고 서비스 범위 로그만 사용합니다.

```bash
sudo docker compose --env-file .env --file compose.yaml ps
sudo docker compose --env-file .env --file compose.yaml logs --tail=200 frontend
sudo docker compose --env-file .env --file compose.yaml logs --tail=200 backend
sudo docker compose --env-file .env --file compose.yaml logs --tail=200 mysql
sudo docker compose --env-file .env --file compose.yaml logs --tail=200 nginx
```

개인정보 포함 여부를 검토하기 전에는 로그를 공유하지 않습니다.

## 백업 및 복원

저장 시 암호화되는 비공개 백업 압축 파일을 생성합니다.

```bash
sudo ./scripts/backup.sh
```

명령은 `/data/community/backup` 아래의 압축 파일 경로를 출력합니다. 운영자가 승인한
암호화 채널을 사용해 백업을 인스턴스 외부로 복사합니다. 복원 훈련을 통과한 적이 없는
백업은 복구 가능하다고 간주하지 않습니다.

복원은 파괴적 작업이므로 명시적인 확인 토큰이 필요합니다.

```bash
sudo RESTORE_ARCHIVE=/data/community/backup/community-<timestamp>.tar.gz \
  RESTORE_CONFIRM=restore-community \
  ./scripts/restore.sh
sudo ./scripts/verify.sh
```

복원에 성공한 후에도 이전 업로드 디렉터리를 보존합니다. 나중에 수동으로 삭제하기 전에
반드시 검토합니다.

## 롤백

`/data/community/releases/previous.manifest`를 읽고 해당 이미지가 로컬에 남아 있는지
확인한 다음 실행합니다.

```bash
sudo ROLLBACK_CONFIRM=rollback-community ./scripts/rollback.sh
```

롤백은 `current.env`와 `previous.env`를 교환하므로 교체된 릴리스를 통제된 재전진에
사용할 수 있습니다. 데이터베이스 마이그레이션은 이전 애플리케이션 이미지와 하위
호환되어야 합니다. 호환되지 않는 스키마에는 별도로 검토한 데이터베이스 복원이
필요합니다.

## 중지 및 재시작

추적 중인 릴리스 환경 파일을 사용합니다.

```bash
sudo docker compose --env-file /data/community/releases/current.env \
  --file compose.yaml restart
sudo docker compose --env-file /data/community/releases/current.env \
  --file compose.yaml down
sudo docker compose --env-file /data/community/releases/current.env \
  --file compose.yaml up --detach --wait
```

바인드 마운트한 MySQL 및 업로드 데이터는 `down`과 컨테이너 교체 후에도 유지됩니다.
`docker compose down -v`를 일상 명령으로 사용하지 않습니다.
