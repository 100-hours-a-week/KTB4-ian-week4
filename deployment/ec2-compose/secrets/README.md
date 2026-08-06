# 실행 환경 비밀값

실제 비밀값은 이 저장소나 Compose 환경 파일에 절대 포함하지 않습니다. EC2에서
`prepare-directories.sh`가 다음과 같은 빈 파일을 생성합니다.

- `/etc/community/secrets/mysql-root-password`
- `/etc/community/secrets/mysql-app-password`
- `/etc/community/secrets/jwt-secret`

파일 소유권은 `root:community-secrets`이고 모드는 `0640`입니다. 보조 그룹 ID `20000`을
명시적으로 할당받은 컨테이너만 마운트된 비밀값을 읽을 수 있습니다. 백엔드와 MySQL은
동일한 원본 파일에서 애플리케이션 데이터베이스 비밀번호를 받고, JWT 비밀값은
백엔드에만 마운트합니다.

EC2에서 대화형으로 값을 입력합니다. 셸 기록이나 명령 인수에는 값을 넣지 않습니다.

```bash
sudoedit /etc/community/secrets/mysql-root-password
sudoedit /etc/community/secrets/mysql-app-password
sudoedit /etc/community/secrets/jwt-secret
```

각 파일에는 따옴표로 감싸지 않은 비어 있지 않은 한 줄만 있어야 합니다. 독립적으로
생성한 데이터베이스 비밀번호를 사용합니다. JWT 값은 최소 32바이트의 무작위 값을
base64로 인코딩한 값이어야 합니다.

내용을 출력하지 않고 메타데이터를 검증합니다.

```bash
sudo stat -c '%U:%G %a %n' /etc/community/secrets/*
sudo test -s /etc/community/secrets/mysql-root-password
sudo test -s /etc/community/secrets/mysql-app-password
sudo test -s /etc/community/secrets/jwt-secret
```

예상 소유권과 모드는 `root:community-secrets 640`입니다.

## TLS 자료

TLS 종료는 엣지 Nginx 컨테이너가 담당하지만 Certbot은 호스트에서 유지합니다.
`/etc/community/tls/fullchain.pem`과 `privkey.pem`은 애플리케이션 비밀값과 분리하며,
소유권 `root:community-tls`(`0:20001`)와 모드 `0640`을 사용해야 합니다.
`certbot-webroot.sh`는 `/data/community/acme`를 사용하고 배포 훅은 원자적 교체 전에
만료일과 인증서·키 쌍을 검증합니다. 활성 컨테이너가 `nginx -t`에서 새 파일을 거부하면
훅은 이전 파일을 복원하고 Nginx를 다시 불러오지 않습니다.
