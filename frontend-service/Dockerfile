# Multi-stage build for React frontend
FROM node:18-alpine AS build
WORKDIR /app

# Install dependencies
COPY package.json ./
RUN npm install --legacy-peer-deps \
	&& npm install --legacy-peer-deps ajv@8 ajv-keywords@5

# Copy source
COPY public ./public
COPY src ./src

# Build production assets
RUN npm run build

# Serve with nginx
FROM nginx:1.27-alpine
# Replace default server config
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy build output
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 3000
CMD ["nginx","-g","daemon off;"]