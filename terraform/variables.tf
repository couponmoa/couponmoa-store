variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS 리전 정보 / .tfvars 참조 "
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_id" {
  description = "VPC 아이디 / .tfvars 참조"
  type        = string
}

variable "internet_gateway_id" {
  description = "게이트웨이 id / .tfvars 참조"
  type        = string
}

variable "ecr_repository_url" {
  description = "ECR 레포지토리 url / .tfvars 참조"
  type        = string
}

variable "max_capacity" {
  description = "오토스케일링 최대 개수 / .tfvars 존재"
  type        = number
}

variable "acm_certificate_arn" {
  description = "인증서 ARN / .tfvars 참조"
  type        = string
}
