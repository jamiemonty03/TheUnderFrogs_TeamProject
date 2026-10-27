resource "aws_ecr_repository" "this" {
  for_each = toset(local.built_images)

  name                 = "${local.name}/${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

# Builds every image and pushes it to ECR whenever image_tag changes.
resource "terraform_data" "images" {
  count            = var.build_images ? 1 : 0
  triggers_replace = [var.image_tag]

  provisioner "local-exec" {
    command     = "${path.module}/scripts/build-and-push.sh ${local.registry} ${local.name} ${var.image_tag}"
    environment = local.exec_env
  }

  depends_on = [aws_ecr_repository.this]
}
