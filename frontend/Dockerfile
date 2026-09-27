# Multi-stage SPA image: Angular production build → nginx (same-origin reverse proxy).
# Build from frontend/: docker build -t succeshub-frontend .

FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json .npmrc ./
# See frontend/.npmrc (legacy-peer-deps) for Tailwind 4 vs Angular 18 peers.
RUN npm ci

COPY . .
RUN npm run build -- --configuration=production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/succes-hub-frontend/browser /usr/share/nginx/html

EXPOSE 80
