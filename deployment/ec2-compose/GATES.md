# PULSE production gates

This repository deliberately stops at local implementation until each gate is
approved. None of the scripts in this directory changes AWS or GitHub settings
on its own.

## Gate 1 — merge and control-plane setup

1. Merge the Backend workflow first, then merge the Frontend workflow.
   While the immutable Frontend baseline does not exist, Backend CI wraps the
   approved legacy source SHA with the CI-only 8080 static-origin adapter in
   `bootstrap/`; it is never published or used by production deployment.
2. Keep both packages public and configure the bootstrap source variables:
   - Backend `FRONTEND_BASELINE_SHA=e8540600908fab04b32965608b6de2bb9b6f85b0`
   - Frontend `BACKEND_BASELINE_SHA=69fb7225ae0e2681b12721c0020d9152021a48d7`
   - Frontend `DEPLOYMENT_BASELINE_SHA` must identify the Backend `main` commit
     that contains `compose.ci.yaml` and the four-service assets. After the
     Backend-first merge, use that merge commit rather than the pre-workflow
     application baseline.
3. After the first successful publications, set `FRONTEND_BASELINE_IMAGE` and
   `BACKEND_BASELINE_IMAGE` to `image@sha256:digest` references. Remove the
   source-build fallback variables only after both Compose smoke jobs pass.
4. Leave `PRODUCTION_DEPLOY_ENABLED=false` and
   `PRODUCTION_DISPATCH_ENABLED=false` during bootstrap.
5. Configure only `BE / required-gate` or `FE / required-gate` as the required
   GitHub Actions check for the corresponding repository. Verify a deliberate
   failure and recovery on a small PR.
6. Configure the Frontend dispatch token, the protected `production`
   Environment, OIDC/IAM, SSM, and the EC2 Secret/TLS files. Do not grant PR
   workflows production Secrets or AWS permissions.

## Gate 2 — read-only production preflight

Install the fixed runner with `install-release-runner.sh`, prepare the host
directories, then run `preflight.sh` with an approved release environment. It
checks file metadata, public image pulls and digests, Compose rendering, the TLS
certificate/key pair and expiration, and `nginx -t`. It does not start or alter
production services.

Expected protected variables are `AWS_REGION`, `AWS_ROLE_TO_ASSUME`,
`PRODUCTION_INSTANCE_ID`, and `PRODUCTION_ORIGIN=https://pulse.gleeze.com`.
Secret files must be root-owned, group `20000`, mode `0640`; TLS files must use
group `20001`, mode `0640`.

## Gate 3 — first cutover

Use the manual `BE / deploy-production` workflow with `operation=deploy` and
both exact commit/digest pairs. Stop the legacy host Nginx immediately before
the approved run so Compose can bind 80/443. A first-deployment failure stops
the partial Compose stack but cannot restart the legacy service; start it again
manually. Later failures reconverge `current.env`, and manual rollback requires
`operation=rollback` plus `rollback-community`.

Database and upload paths are never removed by deploy or rollback. A destructive
Flyway migration is not automatically reversible and requires a separately
reviewed data recovery plan.
