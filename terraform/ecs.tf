resource "aws_cloudwatch_log_group" "gateway_api_ecs_logs" {
  name              = "/ecs/gateway-api-${var.environment}"
  retention_in_days = 7
  tags = {
    Environment = var.environment
  }
}
resource "aws_cloudwatch_log_group" "order_notification_ecs_logs" {
  name              = "/ecs/order-notification-${var.environment}"
  retention_in_days = 7
  tags = {
    Environment = var.environment
  }
}

# ECS
resource "aws_ecs_cluster" "couponmoa_cluster" {
  name = "couponmoa-api-ecs-cluster-${var.environment}"
}

# ECS Task: Order Notification
resource "aws_ecs_task_definition" "order_notification_task_definition" {
  family                   = "order-notification-task-family-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  runtime_platform {
    cpu_architecture = "ARM64"
  }

  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_execution_role.arn

  container_definitions = jsonencode([
    {
      name         = "order-notification-${var.environment}"
      image        = "${var.ecr_repository_url}/order-notification-${var.environment}:latest"
      essential    = true
      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.order_notification_ecs_logs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

# ECS Service: Order Notification
resource "aws_ecs_service" "order_notification_service" {
  name                              = "order-notification-${var.environment}"
  cluster                           = aws_ecs_cluster.couponmoa_cluster.id
  task_definition                   = aws_ecs_task_definition.order_notification_task_definition.arn
  launch_type                       = "FARGATE"
  desired_count                     = 1
  health_check_grace_period_seconds = 180
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_controller {
    type = "ECS"
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets = [aws_subnet.private_subnet_1.id, aws_subnet.private_subnet_2.id]
    security_groups = [aws_security_group.ecs_service_sg.id]
    assign_public_ip = false
  }
}

# ECS Task: Gateway Api
resource "aws_ecs_task_definition" "gateway_api_task_definition" {
  family                   = "gateway-api-task-family-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  runtime_platform {
    cpu_architecture = "ARM64"
  }

  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_execution_role.arn

  container_definitions = jsonencode([
    {
      name         = "gateway-api-${var.environment}"
      image        = "${var.ecr_repository_url}/gateway-api-${var.environment}:latest"
      essential    = true
      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.gateway_api_ecs_logs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

# ECS Service: Gateway Api
resource "aws_ecs_service" "gateway_api_service" {
  name                              = "gateway-api-${var.environment}"
  cluster                           = aws_ecs_cluster.couponmoa_cluster.id
  task_definition                   = aws_ecs_task_definition.gateway_api_task_definition.arn
  launch_type                       = "FARGATE"
  desired_count                     = 1
  health_check_grace_period_seconds = 180
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_controller {
    type = "ECS"
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets = [aws_subnet.private_subnet_1.id, aws_subnet.private_subnet_2.id]
    security_groups = [aws_security_group.ecs_service_sg.id]
    assign_public_ip = false
  }
}
