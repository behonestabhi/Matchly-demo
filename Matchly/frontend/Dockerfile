# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app

# VITE_API_BASE is baked into the static bundle at build time.
ARG VITE_API_BASE=http://localhost:8080/api/v1
ENV VITE_API_BASE=$VITE_API_BASE

COPY package*.json ./
RUN npm ci || npm install
COPY . .
RUN npm run build

# --- Serve stage ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
