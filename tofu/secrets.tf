# Everything the .env files hold that is secret, generated here the way Jenkins and the
# auth-service scripts generate it, and kept in one Secrets Manager secret that tasks read at start-up.

resource "random_password" "db" {
  length  = 32
  special = false
}

# Same format as app/auth-service/scripts/generate-signing-key.sh: a base64-encoded PKCS#8 RSA key.
resource "tls_private_key" "jwt" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "time_static" "jwt" {
  triggers = { key = tls_private_key.jwt.id }
}

# The trade-executor's client secret; the register-client task stores its hash in auth_db.
resource "random_password" "client_secret" {
  length  = 43
  special = false
}

resource "random_password" "grafana" {
  length  = 24
  special = false
}

locals {
  jwt_key_id = "auth-${formatdate("YYYYMMDDhhmmss", time_static.jwt.rfc3339)}"

  # Optional third-party keys are only stored, and only handed to tasks, when they are set.
  # Whether a key is set is not secret, so the filter is nonsensitive and the services map stays usable in for_each.
  optional_secrets = {
    for k, v in {
      STRIPE_SECRET_KEY = var.stripe_secret_key
      ALPACA_KEY_ID     = var.alpaca_key_id
      ALPACA_SECRET_KEY = var.alpaca_secret_key
      AI_API_KEY        = var.ai_api_key
    } : k => v if nonsensitive(v != "")
  }
}

resource "aws_secretsmanager_secret" "app" {
  name                    = "${local.name}/app"
  description             = "Passwords and keys for the TheUnderFrogs services"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode(merge({
    DB_PASSWORD            = random_password.db.result
    JWT_PRIVATE_KEY        = base64encode(tls_private_key.jwt.private_key_pem_pkcs8)
    AUTH_CLIENT_SECRET     = random_password.client_secret.result
    GRAFANA_ADMIN_PASSWORD = random_password.grafana.result
  }, local.optional_secrets))
}
