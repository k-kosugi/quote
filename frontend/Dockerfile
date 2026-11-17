FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1-alpine

# 起動時に QUOTE_URL, CATALOG_URL と DNS リゾルバーを埋め込んで /etc/nginx/conf.d/default.conf を生成する.
# NGINX_ENTRYPOINT_LOCAL_RESOLVERS を設定すると, /etc/resolv.conf のネームサーバーが NGINX_LOCAL_RESOLVERS に入る
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
