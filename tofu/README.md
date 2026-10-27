# Deploying to AWS with OpenTofu

This folder deploys everything in `docker-compose.yml` to AWS managed services. SonarQube is the one exception: compose runs it only under the `quality` profile, for CI.

## Services at a glance

The stack has 13 long-running ECS Fargate services. The frontend is the public entry point for the trading application; the other application services communicate privately inside the VPC.

| Service | What it does | How it is used |
|---|---|---|
| `frontend` | Angular application served by Nginx | Open the public application URL on port 80. Nginx proxies `/api` and `/auth` requests to backend services and uses Cloud Map DNS for service discovery. |
| `auth-service` | Registration, login, JWT/JWKS, refresh tokens, and service-client authentication | Called by the frontend and backend services. Other services validate JWTs using its JWKS endpoint. |
| `accounts-service` | User account balances, cash movements, and top-ups | Called by orders and trade execution; requires Stripe configuration for payment top-ups. |
| `instruments-service` | Instrument catalog, price history, news, and live prices | Read by the frontend and order flow; consumes Kafka market data and can use Alpaca credentials. |
| `orders-service` | Order creation, cancellation, and status | Called by the frontend and trade executor; publishes order events to Kafka. It emits JSON logs. |
| `positions-service` | Portfolio and position data | Called by the frontend and trade executor during settlement. |
| `trade-executor` | Consumes order events, evaluates/fills orders, and settles trades | Internal worker using Kafka and the accounts, instruments, orders, and positions services. |
| `trade-analytics-service` | Periodically loads and aggregates trade data into analytics tables | Background ETL service; reads the service databases and writes the analytics database. |
| `python` | Dash analytics dashboard | Open the admin dashboard URL on port 8085; reads analytics and instrument data. |
| `kafka-ui` | Kafka topic and message inspection UI | Admin-only URL on port 8086. |
| `prometheus` | Metrics collection and query UI | Admin-only URL on port 9090; scrapes configured service and Nginx metrics. |
| `grafana` | Metrics dashboards | Admin-only URL on port 8080; provisioned with Prometheus as its data source. |
| `nginx-exporter` | Exposes Nginx metrics to Prometheus | Internal metrics sidecar-style service; not exposed publicly. |

Terraform also defines four short-lived Fargate setup tasks. `db-init` creates the six databases and loads schemas/seeds; `kafka-init` creates Kafka topics; `instruments-etl` loads initial instrument data; `register-client` registers the `trade-executor` service client in auth-service's database. They are not user-facing services.

### How the pieces connect

```text
Browser -> CloudFront (HTTPS) -> Application Load Balancer -> frontend (Nginx)
                                                     |---- /auth -> auth-service
                                                     |---- /api  -> application services

Private ECS services -> Cloud Map DNS (`<service>.underfrogs.local`)
                    |-> RDS PostgreSQL (six databases on one instance)
                    |-> MSK Kafka (internal broker endpoints)

Terraform -> ECR image build/push -> ECS task definitions and services
          -> Secrets Manager -> ECS injects named secret values at task start
          -> one-off Fargate setup tasks
```

The app's public URL is the CloudFront distribution. CloudFront forwards viewer headers, cookies, and query strings to the frontend; caching is disabled so API/auth responses and personalized content are not cached. The ALB accepts frontend-origin traffic only from CloudFront's origin-facing network and requires a generated origin-verification header. Admin ports 8080, 8085, 8086, and 9090 remain on the ALB and are opened only for the CIDRs in `admin_cidr_blocks`; leave that list empty to keep those listeners closed to inbound traffic.

| docker-compose | AWS |
|---|---|
| 6 `postgres` containers | One RDS Postgres 15 instance holding six databases: `accounts_db`, `instruments_db`, `orders_db`, `positions_db`, `auth_db`, `analytics_db` |
| `kafka` | MSK, 2 brokers, PLAINTEXT inside the VPC |
| `kafka-init`, DB init hooks, `setup-pipeline.sh` | One-off Fargate tasks, run once by `tofu apply` (see `init-tasks.tf`) |
| Every other service | An ECS Fargate service, in private subnets |
| Images built locally | ECR, one repository per image |
| Container names (`auth-service:8081`) | Cloud Map DNS: `auth-service.underfrogs.local:8081` |
| Browser entry point | CloudFront provides the HTTPS app URL and forwards requests to the frontend ALB listener; admin tools remain on ALB ports `:8080`, `:8085`, `:8086`, and `:9090` (restricted to `admin_cidr_blocks`) |
| `.env` files | Secrets Manager. Passwords, the JWT signing key and the trade-executor client secret are generated; third-party keys come from variables. |

`locals.tf` is the place to start reading: it lists every service the way `docker-compose.yml` does.

## Prerequisites

- OpenTofu 1.8 or later, AWS CLI, Docker with BuildKit, Maven, and JDK 21. The image build script uses these tools on the machine running `tofu apply`.
- An AWS CLI profile authorized for the target account `081452990196` in `eu-west-1`. Set `aws_profile` in your ignored `terraform.tfvars`; this same profile is used by the AWS provider and local deployment scripts. Verify it with `aws sts get-caller-identity --profile <profile>` before planning. The returned `Account` must be `081452990196`.
- Permissions to read and manage the resources in this stack: VPC/networking, ECS/Fargate, ECR, RDS, MSK, ELB, Cloud Map, Secrets Manager, CloudWatch Logs, IAM roles/policies and `iam:PassRole`, plus the APIs used by one-off task scripts. An EC2 instance role or AWS profile is only an identity; it still needs these permissions.

## Deploy

```bash
cd tofu
cp terraform.tfvars.example terraform.tfvars   # set admin_cidr_blocks and any API keys
tofu init
tofu plan
tofu apply -var "image_tag=$(git rev-parse --short HEAD)"
```

`terraform.tfvars` is local, ignored by Git, and should be created from `terraform.tfvars.example`. It holds non-secret choices such as region, profile, image tag, and admin IP ranges. Optional third-party keys are Terraform variables too; see **Secrets and state** before supplying them.

What the first apply does, in order:

1. Creates the network, ECR, RDS and MSK. MSK takes about 25–40 minutes.
2. Builds the five jars, then builds all 10 images and pushes them to ECR (`scripts/build-and-push.sh`).
3. Runs the one-off tasks: `db-init` creates the databases and loads schema and seed data, `kafka-init` creates the topics, `instruments-etl` runs the instruments ETL, and `register-client` registers the trade-executor with auth-service.
4. Starts the 13 ECS services.

Then:

```bash
tofu output app_url                       # open it and sign in as demo
tofu output admin_urls
tofu output -raw grafana_admin_password   # user: admin
```

The Java services take a minute or two to become healthy after the apply finishes. CloudFront distribution deployment can take several minutes; use `tofu output -raw app_url` to get its HTTPS URL once Terraform completes.

## Redeploy after a code change

```bash
tofu apply -var "image_tag=$(git rev-parse --short HEAD)"
```

A new tag rebuilds and pushes every image, then rolls each service onto it. Reusing the same tag does nothing. The one-off tasks do not run again: they run only when the database or MSK cluster is recreated, or when `kafka/create-topics.sh` changes.

For a config-only change, inspect the plan first. Terraform updates task definitions/services as appropriate; rebuilding container images is only needed when the image contents change. `build_images = false` disables the local image-build/push step if images have already been built and pushed separately.

## What `tofu apply` does

Terraform files split the stack by responsibility:

| File | Responsibility |
|---|---|
| `providers.tf`, `versions.tf`, `variables.tf` | AWS provider/profile, required OpenTofu/provider versions, and configurable inputs. |
| `cloudfront.tf` | HTTPS CloudFront distribution, request/cache policies, and guarded ALB origin rule. |
| `locals.tf` | Central service catalog: image names, ports, environment, secret references, health checks, and task sizes. Start here when adding or changing a service. |
| `network.tf`, `security.tf` | VPC, public/private subnets, internet/NAT routing, and network access rules. |
| `rds.tf`, `msk.tf` | Shared private PostgreSQL instance and private Kafka cluster. |
| `ecr.tf`, `ecs.tf`, `services.tf` | Image repositories/build trigger, ECS cluster and IAM roles, Cloud Map, task definitions, logging, and Fargate services. |
| `alb.tf` | Public HTTP listeners, target groups, and health checks. |
| `secrets.tf` | Generated credentials and the application secret in Secrets Manager. |
| `init-tasks.tf` | One-off Fargate tasks and their replacement triggers. |
| `outputs.tf` | App/admin URLs, cluster name, database endpoint, and generated Grafana password. |

At a high level, the first apply creates the network and managed services, builds and pushes images, runs database/Kafka/data/client setup tasks, then launches the long-running ECS services. Dependencies in the Terraform graph enforce that order. MSK creation is the slowest part and can take tens of minutes. Later applies update only resources whose inputs or source triggers changed.

### Adding a service

Add an entry to `local.services` in `locals.tf` with its image, Fargate CPU/memory, container port, environment, secret references, and health check. Add the image name to `local.built_images` if this repo builds it. Set `alb_port` and `health_path` only if it needs an ALB listener; otherwise it is private. Add security rules only when the service needs a new network path. The shared task-definition/service resources in `services.tf` create ECS resources from those maps.

### Adding a one-off setup task

Add the task under `local.init_tasks` and give it an appropriate `triggers_replace` rule in `init-tasks.tf`. Its image must be available in ECR, it runs in private subnets, and `scripts/run-task.sh` waits for completion and fails the apply on a non-zero exit. Make dependencies explicit so setup finishes before dependent services start.

## AWS identity and account

OpenTofu uses the AWS provider identity selected by `aws_profile` (or the default credential chain when it is `null`). The profile name does not grant access by itself; the profile must resolve to credentials authorized for the target account.

```bash
aws configure list-profiles
aws sts get-caller-identity --profile training
```

The `Account` should be `081452990196` for this deployment. If OpenTofu reports an unexpected account, stop before applying and check `aws_profile`, the active shell/environment, and the AWS profile configuration. AWS access keys belong in the AWS credential chain, not in Terraform variables or `terraform.tfvars`. Application secrets in Secrets Manager are unrelated to OpenTofu's AWS authentication.

## CloudFront behavior

Visitors use the CloudFront HTTPS URL. CloudFront forwards requests to the frontend listener on the ALB; Nginx continues to proxy API/auth paths. The distribution forwards viewer headers other than `Host`, all cookies, and all query strings. Caching is disabled initially, which is deliberate for login, API, and personalized responses. The ALB security group allows port 80 only from AWS's CloudFront origin-facing prefix list, and its frontend listener returns `403` unless the generated origin-verification header is present. The admin ALB listeners are separate and retain their `admin_cidr_blocks` restrictions.

This gives HTTPS from the browser to CloudFront, but CloudFront currently talks to the ALB over HTTP. For a custom hostname and TLS on the origin hop, add an ALB HTTPS listener and ACM certificate in `eu-west-1`, and configure the CloudFront alias and viewer certificate in `us-east-1`. Static asset caching can be added as a separate behavior after verifying the frontend's cache headers and hashed asset names; keep `/api/*` and `/auth/*` uncached.

## Debugging

- Logs are in CloudWatch under `/ecs/underfrogs/<service>`. For example: `aws logs tail /ecs/underfrogs/orders-service --follow --profile training --region eu-west-1`
- Shell into a running container:
  `aws ecs execute-command --cluster underfrogs --task <task id> --container orders-service --interactive --command sh --profile training --region eu-west-1`
  (this needs the Session Manager plugin for the AWS CLI)
- Rerun a one-off task: `tofu apply -replace=terraform_data.instruments_etl`
- See current services and tasks: `aws ecs list-services --cluster underfrogs --profile training --region eu-west-1` and `aws ecs list-tasks --cluster underfrogs --service-name auth-service --profile training --region eu-west-1`.
- If `tofu plan` fails with `AccessDenied`, inspect the identity first: `aws sts get-caller-identity --profile training`. The AWS role/user needs read permissions for planning and create/update permissions for applying. Errors naming `ecr:DescribeRepositories`, `ec2:DescribeVpcs`, or `iam:GetRole` are IAM authorization problems, not image-build failures.
- If ECS tasks fail to start, check their stopped reason and the matching CloudWatch log group. Common causes include a missing secret key, failed health check, inability to pull an image, or a setup task that exited unsuccessfully.

## Secrets and state

`secrets.tf` creates one Secrets Manager secret named `<project_name>/app`. Terraform generates the database password, JWT private key, trade-executor client secret, and Grafana password; optional Stripe, Alpaca, and AI provider values come from `stripe_secret_key`, `alpaca_key_id`, `alpaca_secret_key`, and `ai_api_key`. `locals.tf` maps each service to only the secret keys it needs. ECS reads these values when a task starts, using its execution role; replacing a secret value does not change an already-running container's environment, so roll/redeploy tasks to load updated values.

With this Terraform-managed design, optional third-party secret values are passed into Terraform and stored in Terraform state, even though variables are marked `sensitive` and hidden from normal CLI output. Sensitive marking is display protection, not state encryption. Never commit `terraform.tfvars`, state files, plan files, or secret values. Rotate any credential that was exposed. The default state is local, so each operator must use the correct state file; separate state files can create duplicate infrastructure or overwrite resources unexpectedly.

For team use, configure a shared encrypted S3 backend with state locking before multiple people run Terraform. The commented example in `versions.tf` is only a starting point: create the bucket with appropriate encryption, versioning, and access controls, then configure the backend and migrate state with `tofu init -migrate-state`. Coordinate this change; do not copy or replace state files casually.

## Cost and teardown

It costs roughly **$250–300 a month** while running before CloudFront request/data-transfer charges: Fargate about $150, MSK $66, NAT gateway $33, load balancer $17, RDS $13, plus small amounts for Secrets Manager, logs and ECR. CloudFront cost depends on traffic and cache behavior. Remove everything with:

```bash
tofu destroy
```

By default `skip_final_snapshot = true`, so destroying deletes the database data. Set it to `false` to keep a final snapshot.

## Things to know

- **Destroy is destructive.** With the default `skip_final_snapshot = true`, RDS data is deleted. Set it to `false` and verify the plan if the database must be retained.
- **CloudFront TLS.** The default CloudFront hostname supports HTTPS for visitors. The CloudFront-to-ALB origin hop currently uses HTTP inside AWS; end-to-end TLS and a custom app domain require an ACM certificate in `eu-west-1` for the ALB plus a viewer certificate in `us-east-1` for CloudFront.
- **CloudFront caching.** Responses are intentionally uncached initially. Add a separate static-asset behavior only after confirming fingerprinted asset URLs and cache headers; keep `/api/*` and `/auth/*` uncached.
- **Prometheus and Grafana keep no data across restarts.** Their config is provisioned, so only metrics history and UI-made changes are lost. Add EFS volumes if that matters.
- **Changes outside this folder:** `app/frontend/nginx.conf` is now a template (`NGINX_RESOLVER`, `SERVICE_DOMAIN`), and `kafka/create-topics.sh` reads `KAFKA_BOOTSTRAP_SERVERS`. The defaults keep docker-compose working exactly as before.
