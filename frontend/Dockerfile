# Production web image.
# The frontend is already built into the dist/ folder, so Docker does not need
# to run npm install during compose build. This avoids npm's "Exit handler never called" bug.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
