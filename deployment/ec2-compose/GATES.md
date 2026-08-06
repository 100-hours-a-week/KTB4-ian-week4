# PULSE 운영 관문

이 저장소는 각 관문이 승인될 때까지 의도적으로 로컬 구현 단계에서 중단합니다. 이
디렉터리의 어떤 스크립트도 AWS 또는 GitHub 설정을 자체적으로 변경하지 않습니다.

## 관문 1 — 병합 및 제어 영역 설정

1. 백엔드 워크플로를 먼저 병합한 다음 프론트엔드 워크플로를 병합합니다.
   변경 불가능한 프론트엔드 기준선이 아직 없을 때 백엔드 CI는 승인된 기존 소스 SHA를
   `bootstrap/`의 CI 전용 8080 정적 원본 어댑터로 감쌉니다. 이 이미지는 발행하거나
   운영 배포에 사용하지 않습니다.
2. 두 패키지를 모두 공개 상태로 유지하고 초기 소스 변수를 설정합니다.
   - 백엔드 `FRONTEND_BASELINE_SHA=e8540600908fab04b32965608b6de2bb9b6f85b0`
   - 프론트엔드 `BACKEND_BASELINE_SHA=69fb7225ae0e2681b12721c0020d9152021a48d7`
   - 프론트엔드 `DEPLOYMENT_BASELINE_SHA`는 `compose.ci.yaml`과 4개 서비스 자산을
     포함하는 백엔드 `main` 커밋을 가리켜야 합니다. 백엔드를 먼저 병합한 후에는
     워크플로 도입 전 애플리케이션 기준선이 아닌 해당 병합 커밋을 사용합니다.
3. 최초 이미지 발행이 성공하면 `FRONTEND_BASELINE_IMAGE`와
   `BACKEND_BASELINE_IMAGE`를 `image@sha256:digest` 참조로 설정합니다. 두 Compose
   스모크 작업이 모두 통과한 뒤에만 소스 빌드 대체 변수를 제거합니다.
4. 초기 구성 중에는 `PRODUCTION_DEPLOY_ENABLED=false`와
   `PRODUCTION_DISPATCH_ENABLED=false`를 유지합니다.
5. 각 저장소의 필수 GitHub Actions 검사에는 `BE / required-gate` 또는
   `FE / required-gate`만 설정합니다. 작은 PR에서 의도적인 실패와 복구를 검증합니다.
6. 프론트엔드 디스패치 토큰, 보호된 `production` 환경, OIDC/IAM, SSM 및 EC2 비밀값·TLS
   파일을 설정합니다. PR 워크플로에는 운영 비밀값이나 AWS 권한을 부여하지 않습니다.

## 관문 2 — 읽기 전용 운영 사전 점검

`install-release-runner.sh`로 고정 실행기를 설치하고 호스트 디렉터리를 준비한 다음,
승인된 릴리스 환경으로 `preflight.sh`를 실행합니다. 이 스크립트는 파일 메타데이터,
공개 이미지 가져오기와 다이제스트, Compose 렌더링, TLS 인증서·키 쌍과 만료일 및
`nginx -t`를 검사합니다. 운영 서비스를 시작하거나 변경하지 않습니다.

필요한 보호 변수는 `AWS_REGION`, `AWS_ROLE_TO_ASSUME`, `PRODUCTION_INSTANCE_ID` 및
`PRODUCTION_ORIGIN=https://pulse.gleeze.com`입니다. 비밀 파일은 루트 소유, 그룹
`20000`, 모드 `0640`이어야 하며 TLS 파일은 그룹 `20001`, 모드 `0640`을 사용해야
합니다.

## 관문 3 — 최초 전환

정확한 두 커밋·다이제스트 쌍과 `operation=deploy`를 입력해 수동
`BE / deploy-production` 워크플로를 사용합니다. 승인된 실행 직전에 기존 호스트
Nginx를 중지해 Compose가 80/443에 바인드할 수 있게 합니다. 최초 배포에 실패하면 일부
기동된 Compose 스택은 중지되지만 기존 서비스를 자동으로 다시 시작할 수는 없으므로
수동으로 재시작합니다. 이후 실패는 `current.env`로 다시 수렴하며, 수동 롤백에는
`operation=rollback`과 `rollback-community`가 필요합니다.

배포나 롤백은 데이터베이스 및 업로드 경로를 제거하지 않습니다. 파괴적인 Flyway
마이그레이션은 자동으로 되돌릴 수 없으므로 별도로 검토한 데이터 복구 계획이 필요합니다.
