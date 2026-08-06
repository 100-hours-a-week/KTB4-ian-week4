# 단일 EC2 배포

이 배포 방식은 H2와 업로드 이미지를 EC2 인스턴스에 보관합니다. 단일 인스턴스 시연에는
적합하지만 수평 확장이나 가용성이 중요한 운영 서비스에는 적합하지 않습니다.

## 필수 인스턴스 설정

1. Java 21을 설치하고 `/usr/bin/java`에서 `java`를 사용할 수 있는지 확인합니다.
2. 서비스 계정과 영구 디렉터리를 생성합니다.

   ```bash
   sudo useradd --system --home /opt/ian-community --shell /usr/sbin/nologin ian-community
   sudo install -d -o ian-community -g ian-community -m 0750 /opt/ian-community
   sudo install -d -o ian-community -g ian-community -m 0700 /var/lib/ian-community/data
   sudo install -d -o ian-community -g ian-community -m 0700 /var/lib/ian-community/storage/images
   sudo install -d -o root -g root -m 0700 /etc/ian-community
   ```

3. `./gradlew bootJar`로 빌드하고 생성된 JAR을 `/opt/ian-community/backend.jar`로
   복사합니다.
4. `deploy/env/backend.env.example`을 `/etc/ian-community/backend.env`로 복사하고,
   도메인을 교체한 뒤 고유한 JWT 서명 키를 설정합니다. Git에 저장하지 않고 다음처럼
   생성합니다.

   ```bash
   openssl rand -base64 48
   ```

5. 환경 파일을 보호합니다.

   ```bash
   sudo chown root:root /etc/ian-community/backend.env
   sudo chmod 0600 /etc/ian-community/backend.env
   ```

6. 서비스를 설치하고 시작합니다.

   ```bash
   sudo cp deploy/systemd/ian-community-backend.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable --now ian-community-backend
   sudo systemctl status ian-community-backend
   ```

운영 프로필은 Spring Boot를 `127.0.0.1:8080`에 바인드합니다. 로컬 역방향 프록시만
접근해야 하며 EC2 보안 그룹이나 Nginx를 통해 포트 8080 또는 `/h2-console`을 노출하지
않습니다.

## 영속성과 백업

- H2 데이터: `/var/lib/ian-community/data`
- 업로드 이미지: `/var/lib/ian-community/storage/images`

두 경로는 모두 영구 EBS 볼륨에 있어야 합니다. 일관된 백업을 만들려면 H2 데이터베이스
파일을 복사하기 전에 서비스를 중지합니다. 복원 절차를 정기적으로 테스트합니다.

## 시작 관문

공개 트래픽을 허용하기 전에 다음을 확인합니다.

- `SPRING_PROFILES_ACTIVE=prod`
- 역방향 프록시에서 HTTPS가 활성화되어 있습니다.
- `FRONTEND_ORIGIN`이 공개 HTTPS 출처와 정확히 일치합니다.
- `JWT_SECRET`이 비어 있지 않고 이 환경에만 사용하는 고유한 값입니다.
- 포트 8080과 22가 전체 인터넷에 공개되지 않습니다.
- EC2 인스턴스에서 IMDSv2 사용이 필수입니다.
- EBS 스냅샷 또는 파일 백업·복원 절차가 존재합니다.
