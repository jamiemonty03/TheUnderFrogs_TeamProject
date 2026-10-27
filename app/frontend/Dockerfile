FROM node:24-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# The image renders templates with envsubst at start-up, filling in only the variables set below.
# The defaults suit docker-compose; ECS overrides them with the VPC resolver and the Cloud Map domain.
ENV NGINX_RESOLVER=127.0.0.11 SERVICE_DOMAIN=""
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
HEALTHCHECK --interval=10s --timeout=5s --retries=6 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1
