terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # TODO: configure a remote backend before any shared/real use. Local state is
  # fine for a first plan, but team use needs locking + a durable store, e.g.:
  #
  # backend "s3" {
  #   bucket         = "matchly-tfstate-<account-id>"
  #   key            = "matchly/<env>/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "matchly-tflock"
  #   encrypt        = true
  # }
}
