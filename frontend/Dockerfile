# Production image — uses pre-built dist/ (run `pnpm install && pnpm build` locally first)
# For full in-Docker build, use Dockerfile.build
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
