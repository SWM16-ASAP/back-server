locals {
  name_prefix          = "llvpt-${var.test_run_id}"
  environment_file_key = "environment/app.env"
  availability_zones   = var.availability_zones
  public_subnets = {
    for index, availability_zone in local.availability_zones :
    availability_zone => cidrsubnet(var.vpc_cidr, 8, index)
  }
  wiremock_mappings = file("${path.module}/../../wiremock/${var.mock_scenario}.json")
  dependency_services = {
    redis = {
      image       = var.redis_image
      port        = 6379
      cpu         = 256
      memory      = 512
      environment = []
      health_check = {
        command     = ["CMD-SHELL", "redis-cli ping | grep PONG"]
        interval    = 10
        timeout     = 5
        retries     = 5
        startPeriod = 10
      }
    }
    mysql = {
      image  = var.mysql_image
      port   = 3306
      cpu    = 512
      memory = 1024
      environment = [
        {
          name  = "MYSQL_RANDOM_ROOT_PASSWORD"
          value = "yes"
        },
        {
          name  = "MYSQL_DATABASE"
          value = "llv_performance_test"
        }
      ]
      health_check = {
        command     = ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 --silent"]
        interval    = 10
        timeout     = 5
        retries     = 10
        startPeriod = 60
      }
    }
    mock = {
      image        = var.mock_image
      port         = 8080
      cpu          = 256
      memory       = 512
      environment  = []
      health_check = null
    }
  }
}
