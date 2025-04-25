output "aws_subnets" {
  value = {
    public_subnet_1  = aws_subnet.public_subnet_1.id,
    public_subnet_2  = aws_subnet.public_subnet_2.id,
    private_subnet_1 = aws_subnet.private_subnet_1.id,
    private_subnet_2 = aws_subnet.private_subnet_2.id,
  }
}

output "order-notification_cloudwatch" {
  value = aws_cloudwatch_log_group.order_notification_ecs_logs.name
}
