# A task definition for every entry in local.services and local.init_tasks, and an ECS service
# for every entry in local.services. See locals.tf for what each entry holds.

resource "aws_ecs_task_definition" "this" {
  for_each = local.task_definitions

  family                   = "${local.name}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    for k, v in {
      name         = each.key
      image        = each.value.image
      essential    = true
      portMappings = [for p in [try(each.value.port, null)] : { containerPort = p, protocol = "tcp" } if p != null]
      environment  = [for k, v in try(each.value.env, {}) : { name = k, value = tostring(v) }]
      secrets      = [for k, v in try(each.value.secrets, {}) : { name = k, valueFrom = v }]
      entryPoint   = try(each.value.entry_point, null)
      command      = try(each.value.command, null)
      healthCheck = try(each.value.health, null) == null ? null : {
        command     = ["CMD-SHELL", each.value.health]
        interval    = 10
        timeout     = 5
        retries     = 10
        startPeriod = try(each.value.start_period, 30)
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.this[each.key].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = each.key
        }
      }
    } : k => v if v != null
  }])
}

resource "aws_service_discovery_service" "this" {
  for_each = { for k, v in local.services : k => v if try(v.port, null) != null }

  name = each.key

  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.this.id
    routing_policy = "MULTIVALUE"

    dns_records {
      type = "A"
      ttl  = 10
    }
  }

}

resource "aws_ecs_service" "this" {
  for_each = local.services

  name                   = each.key
  cluster                = aws_ecs_cluster.this.id
  task_definition        = aws_ecs_task_definition.this[each.key].arn
  desired_count          = var.desired_count
  launch_type            = "FARGATE"
  enable_execute_command = true

  # Java services take a while to start on Fargate; keep the ALB from killing them meanwhile.
  health_check_grace_period_seconds = try(each.value.alb_port, null) == null ? null : 300

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = false
  }

  dynamic "service_registries" {
    for_each = try(each.value.port, null) == null ? [] : [aws_service_discovery_service.this[each.key].arn]
    content {
      registry_arn = service_registries.value
    }
  }

  dynamic "load_balancer" {
    for_each = try(each.value.alb_port, null) == null ? [] : [aws_lb_target_group.this[each.key].arn]
    content {
      target_group_arn = load_balancer.value
      container_name   = each.key
      container_port   = each.value.port
    }
  }

  # Compose's depends_on: services start once the data is in place, as setup-pipeline.sh does.
  depends_on = [
    aws_lb_listener.this,
    aws_iam_role_policy.execution_secrets,
    terraform_data.db_init,
    terraform_data.kafka_init,
    terraform_data.instruments_etl,
    terraform_data.register_client,
  ]
}
