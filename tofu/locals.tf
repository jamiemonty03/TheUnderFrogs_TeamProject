# The deployment, service by service, mirroring docker-compose.yml. ecs.tf turns each entry
# into a task definition, and every entry in `services` also becomes an ECS service.
#
#   image         image to run
#   cpu, memory   Fargate task size (compose mem_limit, rounded up to a Fargate size)
#   port          container port; services with one get a <name>.<project_name>.local DNS name
#   env, secrets  environment, with secrets read from Secrets Manager at start-up
#   health        container health check, as in compose
#   alb_port      ALB listener port, for services compose publishes to the host
#   health_path   ALB health check path

locals {
  name     = var.project_name
  domain   = "${var.project_name}.local"
  registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"

  # Images built from this repo by scripts/build-and-push.sh and pushed to ECR as <project_name>/<name>.
  built_images = [
    "accounts-service", "instruments-service", "orders-service", "positions-service", "trade-executor",
    "auth-service", "trade-analytics-service", "frontend", "python", "db-init",
  ]
  image = { for k in local.built_images : k => "${local.registry}/${local.name}/${k}:${var.image_tag}" }

  # Compose's container names become Cloud Map names, e.g. http://auth-service.underfrogs.local:8081
  url = {
    for k in ["accounts-service", "instruments-service", "orders-service", "positions-service", "auth-service"] :
    k => "http://${k}.${local.domain}:8081"
  }

  public_url = "http://${aws_lb.this.dns_name}"
  kafka      = aws_msk_cluster.this.bootstrap_brokers
  db_host    = aws_db_instance.this.address
  jdbc = {
    for db in ["accounts_db", "instruments_db", "orders_db", "positions_db", "analytics_db"] :
    db => "jdbc:postgresql://${local.db_host}:5432/${db}"
  }

  secret = {
    for k in concat(["DB_PASSWORD", "JWT_PRIVATE_KEY", "AUTH_CLIENT_SECRET", "GRAFANA_ADMIN_PASSWORD"], keys(local.optional_secrets)) :
    k => "${aws_secretsmanager_secret.app.arn}:${k}::"
  }
  alpaca_secrets = { for k in ["ALPACA_KEY_ID", "ALPACA_SECRET_KEY"] : k => local.secret[k] if contains(keys(local.optional_secrets), k) }

  # Ports compose publishes for the browser. 80 is public; the rest only open to admin_cidr_blocks.
  alb_ports = {
    frontend   = 80
    grafana    = 8080
    python     = 8085
    kafka-ui   = 8086
    prometheus = 9090
  }
  admin_ports = [for k, p in local.alb_ports : p if p != 80]

  # x-java-env in docker-compose.yml, plus where the Spring services fetch the auth-service signing keys.
  java_env = {
    JAVA_TOOL_OPTIONS                          = "-XX:MaxRAMPercentage=70 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError"
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = "5"
    AUTH_JWKS_URI                              = "${local.url["auth-service"]}/.well-known/jwks.json"
  }

  services = {
    accounts-service = {
      image        = local.image["accounts-service"]
      cpu          = 512
      memory       = 1024
      port         = 8081
      health       = "wget -q -O /dev/null http://localhost:8081/api/v3/api-docs || exit 1"
      start_period = 120
      env = merge(local.java_env, {
        SPRING_DATASOURCE_URL      = local.jdbc["accounts_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
        FRONTEND_URL               = local.public_url
      })
      secrets = merge(
        { SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"] },
        { for k, v in local.secret : k => v if k == "STRIPE_SECRET_KEY" },
      )
    }

    instruments-service = {
      image        = local.image["instruments-service"]
      cpu          = 512
      memory       = 1024
      port         = 8081
      health       = "wget -q -O /dev/null http://localhost:8081/api/v3/api-docs || exit 1"
      start_period = 120
      env = merge(local.java_env, {
        SPRING_DATASOURCE_URL      = local.jdbc["instruments_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
        KAFKA_BOOTSTRAP_SERVERS    = local.kafka
        ALPACA_NEWS_URL            = "https://data.alpaca.markets/v1beta1/news"
        AI_BASE_URL                = "https://api.groq.com/openai/v1"
        AI_MODEL                   = "openai/gpt-oss-120b"
        AI_FALLBACK_MODEL          = "openai/gpt-oss-20b"
      })
      secrets = merge(
        { SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"] },
        local.alpaca_secrets,
        { for k, v in local.secret : k => v if k == "AI_API_KEY" },
      )
    }

    orders-service = {
      image        = local.image["orders-service"]
      cpu          = 512
      memory       = 1024
      port         = 8081
      health       = "wget -q -O /dev/null http://localhost:8081/api/v3/api-docs || exit 1"
      start_period = 120
      env = merge(local.java_env, {
        SPRING_PROFILES_ACTIVE     = "json-logs"
        SPRING_DATASOURCE_URL      = local.jdbc["orders_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
        KAFKA_BOOTSTRAP_SERVERS    = local.kafka
        ACCOUNTS_SERVICE_URL       = "${local.url["accounts-service"]}/api/accounts"
        INSTRUMENTS_SERVICE_URL    = "${local.url["instruments-service"]}/api/instruments"
        PORTFOLIOS_SERVICE_URL     = "${local.url["positions-service"]}/api/portfolios"
      })
      secrets = { SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"] }
    }

    positions-service = {
      image        = local.image["positions-service"]
      cpu          = 512
      memory       = 1024
      port         = 8081
      health       = "wget -q -O /dev/null http://localhost:8081/api/v3/api-docs || exit 1"
      start_period = 120
      env = merge(local.java_env, {
        SPRING_DATASOURCE_URL      = local.jdbc["positions_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
      })
      secrets = { SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"] }
    }

    trade-executor = {
      image        = local.image["trade-executor"]
      cpu          = 512
      memory       = 1024
      port         = 8081
      health       = "wget -q -O /dev/null http://localhost:8081/actuator/health || exit 1"
      start_period = 120
      env = merge(local.java_env, {
        KAFKA_BOOTSTRAP_SERVERS      = local.kafka
        ORDERS_URL                   = "${local.url["orders-service"]}/api/orders"
        INSTRUMENTS_URL              = "${local.url["instruments-service"]}/api/instruments"
        PRICES_URL                   = "${local.url["instruments-service"]}/api"
        ACCOUNTS_URL                 = "${local.url["accounts-service"]}/api/accounts"
        POSITIONS_URL                = "${local.url["positions-service"]}/api/positions"
        AUTH_TOKEN_URL               = "${local.url["auth-service"]}/auth/token"
        AUTH_CLIENT_ID               = "trade-executor"
        ALPACA_TRADING_URL           = "https://paper-api.alpaca.markets/v2"
        MARKET_DATA_POLL_INTERVAL_MS = "5000"
      })
      secrets = merge({ AUTH_CLIENT_SECRET = local.secret["AUTH_CLIENT_SECRET"] }, local.alpaca_secrets)
    }

    auth-service = {
      image  = local.image["auth-service"]
      cpu    = 256
      memory = 512
      port   = 8081
      health = "curl -f http://localhost:8081/health || exit 1"
      env = {
        DB_HOST              = local.db_host
        DB_PORT              = "5432"
        DB_USERNAME          = "postgres"
        DB_NAME              = "auth_db"
        DB_SSL               = "true"
        JWT_KEY_ID           = local.jwt_key_id
        ACCOUNTS_SERVICE_URL = local.url["accounts-service"]
        CORS_ORIGIN          = local.public_url
      }
      secrets = {
        DB_PASSWORD     = local.secret["DB_PASSWORD"]
        JWT_PRIVATE_KEY = local.secret["JWT_PRIVATE_KEY"]
      }
    }

    trade-analytics-service = {
      image  = local.image["trade-analytics-service"]
      cpu    = 256
      memory = 512
      env = {
        ORDERS_DB_URL           = local.jdbc["orders_db"]
        ORDERS_DB_USERNAME      = "postgres"
        ACCOUNTS_DB_URL         = local.jdbc["accounts_db"]
        ACCOUNTS_DB_USERNAME    = "postgres"
        INSTRUMENTS_DB_URL      = local.jdbc["instruments_db"]
        INSTRUMENTS_DB_USERNAME = "postgres"
        ANALYTICS_DB_URL        = local.jdbc["analytics_db"]
        ANALYTICS_DB_USERNAME   = "postgres"
        ETL_INTERVAL_SECONDS    = "300"
        LOG_LEVEL               = "INFO"
      }
      secrets = {
        ORDERS_DB_PASSWORD      = local.secret["DB_PASSWORD"]
        ACCOUNTS_DB_PASSWORD    = local.secret["DB_PASSWORD"]
        INSTRUMENTS_DB_PASSWORD = local.secret["DB_PASSWORD"]
        ANALYTICS_DB_PASSWORD   = local.secret["DB_PASSWORD"]
      }
    }

    frontend = {
      image       = local.image["frontend"]
      cpu         = 256
      memory      = 512
      port        = 80
      health      = "wget -q -O /dev/null http://localhost/ || exit 1"
      alb_port    = local.alb_ports["frontend"]
      health_path = "/"
      # See app/frontend/Dockerfile: Fargate has no Docker DNS, so nginx uses the VPC resolver and Cloud Map names.
      env = {
        NGINX_RESOLVER = cidrhost(var.vpc_cidr, 2)
        SERVICE_DOMAIN = ".${local.domain}"
      }
    }

    # The Dash dashboard (compose service "python").
    python = {
      image       = local.image["python"]
      cpu         = 512
      memory      = 1024
      port        = 8080
      alb_port    = local.alb_ports["python"]
      health_path = "/"
      env = {
        SPRING_DATASOURCE_URL      = local.jdbc["instruments_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
        ANALYTICS_DB_URL           = local.jdbc["analytics_db"]
        ANALYTICS_DB_USERNAME      = "postgres"
        KAFKA_BROKER               = local.kafka
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"]
        ANALYTICS_DB_PASSWORD      = local.secret["DB_PASSWORD"]
      }
    }

    kafka-ui = {
      image       = "provectuslabs/kafka-ui:latest"
      cpu         = 512
      memory      = 1024
      port        = 8086
      alb_port    = local.alb_ports["kafka-ui"]
      health_path = "/"
      env = {
        SERVER_PORT                       = "8086"
        JAVA_OPTS                         = "-XX:MaxRAMPercentage=70 -XX:MaxMetaspaceSize=128m"
        KAFKA_CLUSTERS_0_NAME             = local.name
        KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS = local.kafka
      }
    }

    # Config comes in through an env var and is written to disk at start-up, in place of compose's bind mount.
    prometheus = {
      image       = "prom/prometheus:v2.55.1"
      cpu         = 256
      memory      = 512
      port        = 9090
      health      = "wget -q -O /dev/null http://localhost:9090/-/healthy || exit 1"
      alb_port    = local.alb_ports["prometheus"]
      health_path = "/-/healthy"
      entry_point = ["/bin/sh", "-c"]
      command = [join(" ", [
        "printf '%s' \"$PROMETHEUS_CONFIG\" > /tmp/prometheus.yml &&",
        "exec /bin/prometheus --config.file=/tmp/prometheus.yml --storage.tsdb.path=/prometheus",
        "--storage.tsdb.retention.time=15d --web.enable-lifecycle",
      ])]
      env = {
        PROMETHEUS_CONFIG = templatefile("${path.module}/templates/prometheus.yml.tftpl", { domain = local.domain })
      }
    }

    grafana = {
      image       = "grafana/grafana:11.3.0"
      cpu         = 256
      memory      = 512
      port        = 3000
      health      = "wget -q -O /dev/null http://localhost:3000/api/health || exit 1"
      alb_port    = local.alb_ports["grafana"]
      health_path = "/api/health"
      entry_point = ["/bin/sh", "-c"]
      command = [join(" ", [
        "mkdir -p \"$GF_PATHS_PROVISIONING/datasources\" &&",
        "printf '%s' \"$GRAFANA_DATASOURCE\" > \"$GF_PATHS_PROVISIONING/datasources/prometheus.yml\" &&",
        "exec /run.sh",
      ])]
      env = {
        GF_SECURITY_ADMIN_USER = var.grafana_admin_user
        GF_USERS_ALLOW_SIGN_UP = "false"
        GF_PATHS_PROVISIONING  = "/tmp/provisioning"
        GRAFANA_DATASOURCE     = templatefile("${path.module}/templates/grafana-datasource.yml.tftpl", { domain = local.domain })
      }
      secrets = { GF_SECURITY_ADMIN_PASSWORD = local.secret["GRAFANA_ADMIN_PASSWORD"] }
    }

    nginx-exporter = {
      image   = "nginx/nginx-prometheus-exporter:1.4.2"
      cpu     = 256
      memory  = 512
      port    = 9113
      command = ["--nginx.scrape-uri=http://frontend.${local.domain}/nginx_status"]
    }
  }

  # One-off tasks init-tasks.tf runs once, in place of the compose init hooks and setup-pipeline.sh.
  init_tasks = {
    # Creates each database and, the first time, loads its schema/*.sql and seed/*.sql (tofu/images/db-init).
    db-init = {
      image  = local.image["db-init"]
      cpu    = 256
      memory = 512
      env = {
        PGHOST     = local.db_host
        PGUSER     = "postgres"
        PGDATABASE = "postgres"
      }
      secrets = { PGPASSWORD = local.secret["DB_PASSWORD"] }
    }

    # kafka/create-topics.sh, run against MSK.
    kafka-init = {
      image       = "apache/kafka:3.9.1"
      cpu         = 256
      memory      = 1024
      entry_point = ["bash", "-c"]
      command     = ["printf '%s' \"$CREATE_TOPICS\" > /tmp/create-topics.sh && exec bash /tmp/create-topics.sh"]
      env = {
        KAFKA_BOOTSTRAP_SERVERS = local.kafka
        CREATE_TOPICS           = file("${path.module}/../kafka/create-topics.sh")
      }
    }

    # The instruments ETL, in the order setup-pipeline.sh runs it.
    instruments-etl = {
      image   = local.image["python"]
      cpu     = 512
      memory  = 2048
      command = ["sh", "-c", "for s in ${join(" ", local.instruments_etl)}; do echo \"Running $s\"; python \"/db/etl/$s\" || exit 1; done"]
      env = {
        SPRING_DATASOURCE_URL      = local.jdbc["instruments_db"]
        SPRING_DATASOURCE_USERNAME = "postgres"
      }
      secrets = { SPRING_DATASOURCE_PASSWORD = local.secret["DB_PASSWORD"] }
    }

    # register-service-client.sh: stores the hash of the trade-executor's client secret in auth_db.
    register-client = {
      image   = local.image["auth-service"]
      cpu     = 256
      memory  = 512
      command = ["node", "-e", file("${path.module}/scripts/register-client.js")]
      env = {
        DB_HOST        = local.db_host
        DB_USERNAME    = "postgres"
        DB_NAME        = "auth_db"
        DB_SSL         = "true"
        AUTH_CLIENT_ID = "trade-executor"
      }
      secrets = {
        DB_PASSWORD        = local.secret["DB_PASSWORD"]
        AUTH_CLIENT_SECRET = local.secret["AUTH_CLIENT_SECRET"]
      }
    }
  }

  instruments_etl = [
    "01-generate_raw_instruments.py",
    "02-generate_raw_prices.py",
    "03-etl_prices.py",
    "04-etl_stocks.py",
    "05-etl_etfs.py",
    "06-etl_bonds.py",
    "etl_instruments_metrics.py",
    "etl_price_metrics.py",
  ]

  task_definitions = merge(local.services, local.init_tasks)
}
