# Matchly — common tasks. Requires Docker (and Docker Compose v2).
.PHONY: help up down logs build ps clean ai-test ai-dev fe-dev

help:        ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  %-12s %s\n", $$1, $$2}'

up:          ## Build and start the whole stack
	cp -n .env.example .env || true
	docker compose up --build -d

down:        ## Stop the stack
	docker compose down

logs:        ## Tail all logs
	docker compose logs -f

ps:          ## Show running services
	docker compose ps

build:       ## Build all images without starting
	docker compose build

clean:       ## Stop and remove volumes (DESTROYS local data)
	docker compose down -v

ai-test:     ## Run the AI service test suite locally
	cd ai-service && python -m pytest -q

ai-dev:      ## Run the AI service locally with reload
	cd ai-service && uvicorn app.main:app --reload --port 8000

fe-dev:      ## Run the frontend dev server
	cd frontend && npm install && npm run dev
