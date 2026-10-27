data "aws_ec2_managed_prefix_list" "cloudfront_origin_facing" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

# AWS's managed no-cache policy. A custom one with all TTLs at 0 can't set the Accept-Encoding flags.
data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

resource "aws_cloudfront_origin_request_policy" "app" {
  name    = "${local.name}-app-forward-all"
  comment = "Forward viewer headers, cookies, and query strings to the app origin."

  headers_config {
    header_behavior = "allExcept"

    headers {
      items = ["Host"]
    }
  }

  cookies_config {
    cookie_behavior = "all"
  }

  query_strings_config {
    query_string_behavior = "all"
  }
}

resource "aws_cloudfront_distribution" "app" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${local.name} application"
  price_class         = "PriceClass_100"
  default_root_object = "index.html"

  origin {
    domain_name = aws_lb.this.dns_name
    origin_id   = "${local.name}-frontend-alb"

    custom_header {
      name  = "X-CloudFront-Origin-Verify"
      value = random_password.cloudfront_origin.result
    }

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "${local.name}-frontend-alb"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods  = ["GET", "HEAD", "OPTIONS"]

    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.app.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  depends_on = [aws_lb_listener_rule.cloudfront_origin]
}

resource "aws_lb_listener_rule" "cloudfront_origin" {
  listener_arn = aws_lb_listener.this["frontend"].arn
  priority     = 1

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this["frontend"].arn
  }

  condition {
    http_header {
      http_header_name = "X-CloudFront-Origin-Verify"
      values           = [random_password.cloudfront_origin.result]
    }
  }
}