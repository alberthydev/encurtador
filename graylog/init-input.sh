#!/bin/sh
# Cria automaticamente o input GELF UDP no Graylog (sem intervenção manual).
GL="http://graylog:9000"
AUTH="admin:admin"
TITLE="url-shortener-gelf-udp"

echo "[graylog-init] Aguardando Graylog ficar disponível..."
until curl -sf "$GL/api/system/lbstatus" >/dev/null 2>&1; do
  sleep 5
done
echo "[graylog-init] Graylog no ar."

if curl -sf -u "$AUTH" "$GL/api/system/inputs" | grep -q "$TITLE"; then
  echo "[graylog-init] Input '$TITLE' já existe. Nada a fazer."
  exit 0
fi

PAYLOAD='{"title":"'"$TITLE"'","type":"org.graylog2.inputs.gelf.udp.GELFUDPInput","global":true,"configuration":{"bind_address":"0.0.0.0","port":12201,"recv_buffer_size":262144,"decompress_size_limit":8388608,"number_worker_threads":2}}'

until curl -sf -u "$AUTH" \
    -H "Content-Type: application/json" \
    -H "X-Requested-By: graylog-init" \
    -X POST "$GL/api/system/inputs" -d "$PAYLOAD" >/dev/null 2>&1; do
  echo "[graylog-init] Falha ao criar input, tentando novamente..."
  sleep 5
done
echo "[graylog-init] Input '$TITLE' criado com sucesso."
