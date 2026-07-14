locals {
  name_prefix        = "llvpt-${var.test_run_id}"
  availability_zones = var.availability_zones
  public_subnets = {
    for index, availability_zone in local.availability_zones :
    availability_zone => cidrsubnet(var.vpc_cidr, 8, index)
  }
}
