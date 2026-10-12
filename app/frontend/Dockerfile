FROM node:24-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
HEALTHCHECK --interval=10s --timeout=5s --retries=6 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1
