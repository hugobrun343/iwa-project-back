### Multi-stage Dockerfile for stripe-backend (TypeScript)
FROM node:18 AS builder
WORKDIR /usr/src/app

# Copy package files and install all deps (including dev for build)
COPY package.json package-lock.json* ./
RUN npm install --silent

# Copy sources and build
COPY . .
# Generate Prisma client so @prisma/client files exist for runtime
RUN npx prisma generate
RUN npm run build

### Production image
FROM node:18-bullseye-slim AS runner
WORKDIR /usr/src/app

# Ensure system OpenSSL / libssl are present for Prisma native engine
RUN apt-get update \
	&& apt-get install -y --no-install-recommends ca-certificates openssl libssl1.1 \
	&& rm -rf /var/lib/apt/lists/*

# Install Prisma CLI for db push (needed at runtime)
COPY package.json package-lock.json* ./
RUN npm install --silent && \
    npm install -g prisma@^5.8.0

# Copy built files
COPY --from=builder /usr/src/app/dist ./dist
# Copy the generated Prisma client artifacts from the builder stage so the
# production image has the generated client available at runtime.
COPY --from=builder /usr/src/app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /usr/src/app/node_modules/@prisma ./node_modules/@prisma

# Copy Prisma schema for db push at runtime
COPY --from=builder /usr/src/app/prisma ./prisma

EXPOSE 8088

ENV NODE_ENV=production

# Initialize database schema and start server
CMD sh -c "echo 'Waiting for database connection...' && \
           for i in \$(seq 1 30); do \
             if prisma db push --schema=prisma/schema.prisma --skip-generate --accept-data-loss > /dev/null 2>&1; then \
               echo '✅ Database schema ready!' && \
               break; \
             fi; \
             if [ \$i -eq 30 ]; then \
               echo '⚠️ Warning: Could not connect after 30 attempts, attempting schema push anyway...' && \
               prisma db push --schema=prisma/schema.prisma --skip-generate --accept-data-loss || true; \
               break; \
             fi; \
             echo \"Database not ready, waiting... (attempt \$i/30)\" && \
             sleep 2; \
           done && \
           node dist/server.js"
