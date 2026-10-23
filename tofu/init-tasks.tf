# One-off Fargate tasks that do what the compose init hooks and setup-pipeline.sh do locally.
# Each runs once, when first created, or again when its trigger changes. scripts/run-task.sh
# waits for the task and fails the apply if it exits non-zero.

locals {
  run_task = join(" ", [
    "${path.module}/scripts/run-task.sh",
    aws_ecs_cluster.this.name,
    join(",", aws_subnet.private[*].id),
    aws_security_group.tasks.id,
  ])
}

resource "terraform_data" "db_init" {
  triggers_replace = [aws_db_instance.this.id]

  provisioner "local-exec" {
    command     = "${local.run_task} ${aws_ecs_task_definition.this["db-init"].arn}"
    environment = local.exec_env
  }

  # Tasks run in the private subnets, so they need the NAT route and the images in ECR first.
  depends_on = [
    terraform_data.images,
    aws_route_table_association.private,
    aws_iam_role_policy.execution_secrets,
    aws_secretsmanager_secret_version.app,
  ]
}

resource "terraform_data" "kafka_init" {
  triggers_replace = [aws_msk_cluster.this.id, filesha256("${path.module}/../kafka/create-topics.sh")]

  provisioner "local-exec" {
    command     = "${local.run_task} ${aws_ecs_task_definition.this["kafka-init"].arn}"
    environment = local.exec_env
  }

  # Tasks run in the private subnets, so they need the NAT route and the images in ECR first.
  depends_on = [
    terraform_data.images,
    aws_route_table_association.private,
    aws_iam_role_policy.execution_secrets,
    aws_secretsmanager_secret_version.app,
  ]
}

resource "terraform_data" "instruments_etl" {
  triggers_replace = [aws_db_instance.this.id]

  provisioner "local-exec" {
    command     = "${local.run_task} ${aws_ecs_task_definition.this["instruments-etl"].arn}"
    environment = local.exec_env
  }

  depends_on = [terraform_data.db_init]
}

resource "terraform_data" "register_client" {
  triggers_replace = [aws_db_instance.this.id, sha256(random_password.client_secret.result)]

  provisioner "local-exec" {
    command     = "${local.run_task} ${aws_ecs_task_definition.this["register-client"].arn}"
    environment = local.exec_env
  }

  depends_on = [terraform_data.db_init]
}
