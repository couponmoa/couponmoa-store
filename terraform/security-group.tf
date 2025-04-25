# Security Group for ALB
resource "aws_security_group" "ecs_alb_sg" {
  name        = "couponmoa-ecs-alb-sg-${var.environment}"
  description = "couponmoa-ecs-alb-sg-${var.environment}"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "couponmoa-ecs-alb-sg-${var.environment}"
  }
}

resource "aws_security_group" "ecs_service_sg" {
  name        = "couponmoa-ecs-sg-${var.environment}"
  description = "couponmoa-ecs-sg-${var.environment}"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_alb_sg.id]
  }

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_alb_sg.id]
  }

  ingress {
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_alb_sg.id]
  }

  ingress {
    from_port       = 8082
    to_port         = 8082
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_alb_sg.id]
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "couponmoa-ecs-service-sg-${var.environment}"
  }
}

resource "aws_security_group" "db_sg" {
  name        = "couponmoa-db-sg-${var.environment}"
  description = "Security group for databases in ${var.environment} environment"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_service_sg.id, "sg-091a7c975f6c7e552"] // sg-0572061d9b5a89f51
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "couponmoa-db-sg-${var.environment}"
  }
}
