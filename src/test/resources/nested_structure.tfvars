# Complex nested structure
application_config = {
  frontend = {
    instances = 2
    size      = "t3.medium" # hi
    zones     = ["us-west-2a", "us-west-2b"]
    scaling   = {
      min     = 1
      max     = 5 # hi again
      desired = 2
    }
  }
  backend = {
    instances = 3
    size      = "t3.large"
    zones     = ["us-west-2a", "us-west-2b", "us-west-2c"] # hi some more
    scaling   = {
      min     = 2
      max     = 6
      desired = 3
    }
  }
}
