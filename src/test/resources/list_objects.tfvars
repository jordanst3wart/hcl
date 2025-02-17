# List of objects
subnet_configs = [
  {
    name          = "public-1"
    cidr_block    = "10.0.1.0/24"
    is_public     = true
    route_table   = "public"
  },
  {
    name          = "private-1"
    cidr_block    = "10.0.2.0/24"
    is_public     = false
    route_table   = "private"
  }
]
