# Build stage
FROM node:24-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the production bundle into dist/frontend/browser
RUN npm run build

# Runtime stage: nginx serves the app and proxies API calls to the services
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built application from builder
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=6 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1
