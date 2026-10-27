# One public load balancer with a listener per port compose publishes to the browser
# (see local.alb_ports). The frontend's nginx proxies all /api and /auth traffic, as in compose.

locals {
  alb_services = {
    frontend = {
      alb_port    = local.alb_ports["frontend"]
      port        = 80
      health_path = "/"
    }
    grafana = {
      alb_port    = local.alb_ports["grafana"]
      port        = 3000
      health_path = "/api/health"
    }
    python = {
      alb_port    = local.alb_ports["python"]
      port        = 8080
      health_path = "/"
    }
    kafka-ui = {
      alb_port    = local.alb_ports["kafka-ui"]
      port        = 8086
      health_path = "/"
    }
    prometheus = {
      alb_port    = local.alb_ports["prometheus"]
      port        = 9090
      health_path = "/-/healthy"
    }
  }
}

resource "aws_lb" "this" {
  name               = local.name
  load_balancer_type = "application"
  subnets            = aws_subnet.public[*].id
  security_groups    = [aws_security_group.alb.id]

  # The live price stream is a long-lived websocket; see the nginx proxy_read_timeout.
  idle_timeout = 3600
}

resource "aws_lb_target_group" "this" {
  for_each = local.alb_services

  name        = "${local.name}-${each.key}"
  port        = each.value.port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  deregistration_delay = 30

  health_check {
    path                = each.value.health_path
    matcher             = "200-399"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
}

resource "aws_lb_listener" "this" {
  for_each = local.alb_services

  load_balancer_arn = aws_lb.this.arn
  port              = each.value.alb_port
  protocol          = "HTTP"

  default_action {
    type             = each.key == "frontend" ? "fixed-response" : "forward"
    target_group_arn = each.key == "frontend" ? null : aws_lb_target_group.this[each.key].arn

    dynamic "fixed_response" {
      for_each = each.key == "frontend" ? [true] : []

      content {
        content_type = "text/plain"
        message_body = "Forbidden"
        status_code  = "403"
      }
    }
  }
}
