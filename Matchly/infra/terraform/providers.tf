provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "matchly"
      Environment = var.env
      ManagedBy   = "terraform"
    }
  }
}
