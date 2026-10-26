#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
PORT="${PORT:-8000}" python3 - <<'PY'
import http.server
import os
import socketserver

PORT = int(os.environ.get("PORT", "8000"))


class Handler(http.server.SimpleHTTPRequestHandler):
    def translate_path(self, path):
        # Mirror the GitHub Pages project path (https://agzes.github.io/AFR/)
        # so absolute /AFR/... links also work locally.
        if path == "/AFR" or path.startswith("/AFR/"):
            path = path[len("/AFR") :] or "/"
        return super().translate_path(path)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def send_head(self):
        for header in ("If-Modified-Since", "If-None-Match"):
            if header in self.headers:
                del self.headers[header]
        return super().send_head()


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


print("AFR site: http://127.0.0.1:%d/" % PORT, flush=True)
print("AFR API:  http://127.0.0.1:%d/api/*" % PORT, flush=True)
with Server(("127.0.0.1", PORT), Handler) as httpd:
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
PY
