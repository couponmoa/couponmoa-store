# 퍼블릭 서브넷 1 (AZ a)
# 인터넷 연결이 가능한 공간, 예: 웹 서버, NAT 게이트웨이 설치 위치
resource "aws_subnet" "public_subnet_1" {
  vpc_id            = var.vpc_id  # 기존 VPC에 연결
  cidr_block        = "10.0.100.0/24"  # IP 대역
  availability_zone = "${var.aws_region}a"
  tags = {
    Environment = var.environment
  }
}

# 퍼블릭 서브넷 2 (AZ c)
resource "aws_subnet" "public_subnet_2" {
  vpc_id            = var.vpc_id
  cidr_block        = "10.0.101.0/24"
  availability_zone = "${var.aws_region}c"
  tags = {
    Environment = var.environment
  }
}

# 프라이빗 서브넷 1 (AZ a)
# 인터넷 직접 연결 안 됨. 내부 DB/캐시용
resource "aws_subnet" "private_subnet_1" {
  vpc_id            = var.vpc_id
  cidr_block        = "10.0.200.0/24"
  availability_zone = "${var.aws_region}a"
  tags = {
    Environment = var.environment
  }
}

# 프라이빗 서브넷 2 (AZ c)
resource "aws_subnet" "private_subnet_2" {
  vpc_id            = var.vpc_id
  cidr_block        = "10.0.201.0/24"
  availability_zone = "${var.aws_region}c"
  tags = {
    Environment = var.environment
  }
}

# Redis용 서브넷 그룹
# Redis는 프라이빗 서브넷 안에 배치됨 (1, 2 중 하나 또는 둘 다)
resource "aws_elasticache_subnet_group" "couponmo_private_subnet_group" {
  name        = "couponmo-elasticache-private-subnet-group-${var.environment}"
  description = "ElastiCache Private Subnet Group for couponmo ${var.environment}"
  subnet_ids  = [
    aws_subnet.private_subnet_1.id,
    aws_subnet.private_subnet_2.id,
  ]
  tags = {
    Environment = var.environment
  }
}

# RDS용 서브넷 그룹
# RDS도 Redis와 같은 프라이빗 서브넷에 배치됨
resource "aws_db_subnet_group" "couponmo_db_subnet_group" {
  name        = "couponmo-db-subnet-group-${var.environment}"
  description = "DB Subnet Group for couponmo ${var.environment}"
  subnet_ids  = [
    aws_subnet.private_subnet_1.id,
    aws_subnet.private_subnet_2.id,
  ]
  tags = {
    Environment = var.environment
  }
}

# 퍼블릭 라우팅 테이블
# 퍼블릭 서브넷이 인터넷으로 나갈 때 사용하는 안내판
resource "aws_route_table" "couponmo_public_route_table" {
  vpc_id = var.vpc_id
  route {
    cidr_block = "0.0.0.0/0"  # 모든 외부 트래픽
    gateway_id = var.internet_gateway_id  # 인터넷 게이트웨이로 나감
  }
}

# public_subnet_1에 퍼블릭 라우팅 테이블 연결
resource "aws_route_table_association" "public_route_table_association_1" {
  subnet_id      = aws_subnet.public_subnet_1.id
  route_table_id = aws_route_table.couponmo_public_route_table.id
}

# public_subnet_2에 퍼블릭 라우팅 테이블 연결
resource "aws_route_table_association" "public_route_table_association_2" {
  subnet_id      = aws_subnet.public_subnet_2.id
  route_table_id = aws_route_table.couponmo_public_route_table.id
}

# 고정된 공인 IP (Elastic IP) 생성
# NAT 게이트웨이에 붙여서 외부 응답 받을 수 있도록 함
resource "aws_eip" "eip" {
  domain     = "vpc"  # VPC용 EIP
  depends_on = [var.internet_gateway_id]  # IGW가 먼저 있어야 함
  lifecycle {
    create_before_destroy = true  # 바꿀 때 중단 없이 생성되게
  }
  tags = {
    Name = "${var.environment}-eip"
  }
}

# NAT 게이트웨이 생성
# 퍼블릭 서브넷에 위치하며, 프라이빗 서브넷이 인터넷 나갈 수 있게 도와줌
resource "aws_nat_gateway" "nat_gateway" {
  subnet_id     = aws_subnet.public_subnet_1.id  # 퍼블릭 서브넷에 설치
  allocation_id = aws_eip.eip.id  # 위에서 만든 EIP 연결
  tags = {
    Name = "${var.environment}-gateway"
  }
}

# 프라이빗 라우팅 테이블
# 프라이빗 서브넷이 인터넷 나갈 때 NAT 게이트웨이를 거쳐 나가게 설정
resource "aws_route_table" "couponmo_private_route_table" {
  vpc_id = var.vpc_id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat_gateway.id
  }
}

# private_subnet_1에 프라이빗 라우팅 테이블 연결
resource "aws_route_table_association" "private_route_table_association_1" {
  subnet_id      = aws_subnet.private_subnet_1.id
  route_table_id = aws_route_table.couponmo_private_route_table.id
}

# private_subnet_2에 프라이빗 라우팅 테이블 연결
resource "aws_route_table_association" "private_route_table_association_2" {
  subnet_id      = aws_subnet.private_subnet_2.id
  route_table_id = aws_route_table.couponmo_private_route_table.id
}