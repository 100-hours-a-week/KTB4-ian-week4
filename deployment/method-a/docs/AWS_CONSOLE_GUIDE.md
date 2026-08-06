# AWS 콘솔 수동 준비 가이드

이 문서는 사용자가 AWS 관리 콘솔에서 직접 수행한다. Codex는
AWS 계정이나 EC2에 접근하지 않는다. 리전은 모든 화면에서
`Asia Pacific (Seoul) / ap-northeast-2`인지 확인한다.

## [AWS 콘솔 작업 필요]

현재 단계: 비용 경보와 EC2 보안 기반 준비

목적: 월 최대 비용과 최소 권한을 먼저 고정한다.

콘솔 경로:

1. `Billing and Cost Management > Budgets > Create budget`
2. `IAM > Roles > Create role`
3. `EC2 > Security Groups > Create security group`
4. `EC2 > Instances > Launch instances`

입력값:

### 1. 예산

- 예산 설정: `Customize (advanced)`
- 예산 유형: `Cost budget`
- 기간: `Monthly`
- 갱신: `Recurring`
- 예산 산정 방식: `Fixed`
- 예산 금액: `15 USD`
- 알림: `12 USD`, `Absolute value`, `Actual`
- 추가 권장 알림: `12 USD`, `Forecasted`
- 이메일 수신자: 사용자가 직접 입력하고 확인
- 예산 작업: 생성하지 않음

예산은 강제 지출 제한이 아니며 결제 데이터 반영이 지연될 수 있다.

### 2. EC2 IAM 역할

- 신뢰할 수 있는 엔터티: `AWS service`
- 사용 사례: `EC2`
- 권한 정책: `AmazonSSMManagedInstanceCore` 하나만
- 역할 이름: `community-ec2-ssm-role`

`AdministratorAccess`, `PowerUserAccess`, S3·RDS·비밀값 관리자 권한은
추가하지 않는다.

### 3. 보안 그룹

- 이름: `community-method-a-sg`
- VPC: 인스턴스를 생성할 VPC
- 초기 인바운드 규칙: 없음
- 아웃바운드: 초기 설치와 SSM 통신이 가능한 기존 기본 아웃바운드 유지

배포 검증 직전 다음 규칙만 검토한다.

- HTTP 80: Dynu·Let's Encrypt 적용 시 `0.0.0.0/0`. HTTP-01 인증과
  HTTPS 리디렉션에 계속 필요
- HTTPS 443: 공개 HTTPS 적용 시 `0.0.0.0/0`
- SSH 22: 기본적으로 없음
- 8080, 3306: 절대 추가하지 않음
- IPv6를 사용하지 않으면 `::/0` 규칙을 추가하지 않음

산출물 전송 때문에 SSH가 불가피하면 22를 현재 공인 IP `/32`에만
잠시 열고 전송 직후 삭제한다. `0.0.0.0/0`은 사용하지 않는다.

### 4. EC2

- 이름: `community-method-a`
- AMI: `Ubuntu Server 24.04 LTS`, `64-bit (x86)`
- 인스턴스 유형: `t3a.small`
- 키 페어: 세션 관리자만 사용하면 없이 진행할 수 있다. 임시 SCP가
  필요하면 전용 키 페어를 생성하고 비공개 키는 로컬에서만 보호한다.
- 네트워크: 퍼블릭 서브넷, 퍼블릭 IP 자동 할당 활성화
- 보안 그룹: `community-method-a-sg`
- 스토리지: 루트 `20 GiB`, `gp3`, 암호화 활성화
- IAM 인스턴스 프로파일: `community-ec2-ssm-role`
- 사용자 데이터: 비워 둠
- 종료 방지: 활성화
- 종료 동작: `Stop`
- 크레딧 사양: `Standard`로 설정해 잉여 CPU 크레딧 비용 방지
- 메타데이터 접근 가능: `Enabled`
- 메타데이터 버전: `V2 only (token required)`
- 메타데이터 응답 홉 제한: `1`
- 메타데이터 태그: 비활성화

태그:

- `Name=community-method-a`
- `Project=community`
- `Environment=test`
- `Method=direct-install`

보안 주의사항:

- 루트 계정은 사용하지 않고 콘솔 자격 증명에 MFA를 적용한다.
- 사용자 데이터, 태그, 이름에 비밀값이나 개인정보를 입력하지 않는다.
- 키, 실제 환경 파일, DB/JWT 비밀값을 캡처하거나 공유하지 않는다.
- 공개 IPv4는 고정되지 않으며 중지/시작 후 변경될 수 있다.
- Dynu 적용 후에는 systemd 타이머가 바뀐 공개 IPv4를 같은 `pulse` 호스트에
  반영한다. Dynu IP 업데이트 비밀번호나 해시는 태그·사용자 데이터에 넣지 않는다.
- 22·8080·3306의 외부 공개는 완료 차단 `FAIL`이다.

비용 영향:

- `t3a.small`을 서울 리전에서 24시간 계속 실행하면 15달러 목표를
  초과할 가능성이 높다. 공개 IPv4와 gp3 비용도 별도다.
- 시작 화면 예상 비용을 확인하고 월 15달러 초과 예상이면 생성하지
  말고 결과를 전달한다.
- 비용 상한을 지키려면 사용하지 않을 때 인스턴스를 수동 중지한다.
  중지 중에도 EBS 비용은 계속 발생한다.
- 예산 경보는 즉시 차단 장치가 아니다.

완료 확인:

- 예산 15 USD와 12 USD 실제 경보가 보임
- 역할에는 `AmazonSSMManagedInstanceCore`만 연결됨
- 루트 EBS에 `Encrypted: Yes`
- 메타데이터 옵션에 `IMDSv2: Required`, 홉 제한 `1`
- 보안 그룹에 22·8080·3306 규칙 없음
- `EC2 > Instances > Connect > Session Manager` 탭 연결 가능

완료 후 전달할 정보:

- 비밀값을 제외한 통과/경고 결과
- 예산/역할/EBS/IMDS/보안 그룹 항목별 상태
- 인스턴스 ID와 공개 IP는 필요하면 마스킹
- 예상 월 비용
- 세션 관리자 연결 성공 여부

## 비용 상충 시 중단 기준

고정 사양 `t3a.small`, gp3 20GiB, 공개 IPv4의 콘솔 예상치가 월
15달러를 넘으면 비용 `WARN`이 아니라 요구사항 충돌이다. 인스턴스를
계속 실행하지 말고, 월 예상 실행 시간 또는 사양 변경 승인을 먼저
결정한다.

## 공식 참고

- <https://docs.aws.amazon.com/cost-management/latest/userguide/create-cost-budget.html>
- <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-IMDS-new-instances.html>
- <https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html>
- <https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-getting-started-instance-profile.html>
