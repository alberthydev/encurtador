#!/bin/sh
# Gera tráfego contínuo para popular dashboards do Grafana e logs do Graylog.
APP="${APP_URL:-http://app:8080}"
echo "[loadgen] Gerando tráfego para $APP"
i=0
while true; do
  i=$((i + 1))

  # 201 Created
  resp=$(curl -s -X POST "$APP/api/v1/urls" -H 'Content-Type: application/json' \
         -d "{\"url\":\"https://example.com/page/$i\"}")
  code=$(echo "$resp" | sed -n 's/.*"shortCode":"\([^"]*\)".*/\1/p')

  if [ -n "$code" ]; then
    for n in 1 2 3; do curl -s -o /dev/null "$APP/$code"; done      # 302
    curl -s -o /dev/null "$APP/api/v1/urls/$code"                    # 200
  fi

  curl -s -o /dev/null "$APP/nao-existe-$i"                          # 404
  curl -s -o /dev/null -X POST "$APP/api/v1/urls" \
       -H 'Content-Type: application/json' -d '{"url":"invalida"}'   # 400

  if [ $((i % 5)) -eq 0 ]; then curl -s -o /dev/null "$APP/api/v1/demo/error"; fi   # 500
  if [ $((i % 7)) -eq 0 ]; then curl -s -o /dev/null "$APP/api/v1/demo/logs"; fi    # logs DEBUG/INFO/WARN/ERROR

  sleep 1
done
