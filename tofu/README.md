# Deploying to AWS with OpenTofu

This folder deploys everything in `docker-compose.yml` to AWS managed services. SonarQube is the one exception: compose runs it only under the `quality` profile, for CI.

| docker-compose | AWS |
|---|---|
| 6 `postgres` containers | One RDS Postgres 15 instance holding six databases: `accounts_db`, `instruments_db`, `orders_db`, `positions_db`, `auth_db`, `analytics_db` |
| `kafka` | MSK, 2 brokers, PLAINTEXT inside the VPC |
| `kafka-init`, DB init hooks, `setup-pipeline.sh` | One-off Fargate tasks, run once by `tofu apply` (see `init-tasks.tf`) |
| Every other service | An ECS Fargate service, in private subnets |
| Images built locally | ECR, one repository per image |
| Container names (`auth-service:8081`) | Cloud Map DNS: `auth-service.underfrogs.local:8081` |
| Published ports | Public load balancer: `:80` app; `:8080` Grafana, `:8085` dashboard, `:8086` Kafka UI, `:9090` Prometheus (admin ports only open to `admin_cidr_blocks`) |
| `.env` files | Secrets Manager. Passwords, the JWT signing key and the trade-executor client secret are generated; third-party keys come from variables. |

`locals.tf` is the place to start reading: it lists every service the way `docker-compose.yml` does.

## Prerequisites

- OpenTofu 1.8 or later, the AWS CLI, Docker (with BuildKit), Maven and JDK 21, the same tools the Jenkins build uses.
- AWS credentials that can create VPC, EC2 networking, ECS, ECR, RDS, MSK, ELB, Cloud Map, Secrets Manager, CloudWatch Logs and IAM roles. The build EC2 instance's role (`backend-api-ec2-ssm-role`) cannot; it is not allowed even `ec2:DescribeAvailabilityZones`. Set `aws_profile` in `terraform.tfvars` to the AWS CLI profile you deploy with (`aws configure list-profiles`), and tofu and its scripts will use it in any terminal.

## Deploy

```bash
cd tofu
cp terraform.tfvars.example terraform.tfvars   # set admin_cidr_blocks and any API keys
tofu init
tofu plan
tofu apply -var "image_tag=$(git rev-parse --short HEAD)"
```

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

The Java services take a minute or two to become healthy after the apply finishes.

## Redeploy after a code change

```bash
tofu apply -var "image_tag=$(git rev-parse --short HEAD)"
```

A new tag rebuilds and pushes every image, then rolls each service onto it. Reusing the same tag does nothing. The one-off tasks do not run again: they run only when the database or MSK cluster is recreated, or when `kafka/create-topics.sh` changes.

## Debugging

- Logs are in CloudWatch under `/ecs/underfrogs/<service>`. For example: `aws logs tail /ecs/underfrogs/orders-service --follow`
- Shell into a running container:
  `aws ecs execute-command --cluster underfrogs --task <task id> --container orders-service --interactive --command sh`
  (this needs the Session Manager plugin for the AWS CLI)
- Rerun a one-off task: `tofu apply -replace=terraform_data.instruments_etl`

## Cost and teardown

It costs roughly **$250–300 a month** while running: Fargate about $150, MSK $66, NAT gateway $33, load balancer $17, RDS $13, plus small amounts for Secrets Manager, logs and ECR. Remove everything with:

```bash
tofu destroy
```

By default `skip_final_snapshot = true`, so destroying deletes the database data. Set it to `false` to keep a final snapshot.

## Things to know

- **State holds secrets** (DB password, signing key). It is local by default and git-ignored. To share it with the team, create an S3 bucket and uncomment the `backend "s3"` block in `versions.tf`, then run `tofu init -migrate-state`.
- **HTTP only.** There is no domain or certificate yet. HTTPS needs an ACM certificate and a `:443` listener.
- **Prometheus and Grafana keep no data across restarts.** Their config is provisioned, so only metrics history and UI-made changes are lost. Add EFS volumes if that matters.
- **Changes outside this folder:** `app/frontend/nginx.conf` is now a template (`NGINX_RESOLVER`, `SERVICE_DOMAIN`), and `kafka/create-topics.sh` reads `KAFKA_BOOTSTRAP_SERVERS`. The defaults keep docker-compose working exactly as before.
