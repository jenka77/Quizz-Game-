#!/bin/sh

# Schreibe Umgebungsvariablen in eine JS-Datei
cat <<EOF > /usr/share/nginx/html/env.js
window.__ENV__ = {
  MQTT_BROKER_URL:      "${MQTT_BROKER_URL}",
  MQTT_BROKER_PORT:     "${MQTT_BROKER_PORT}",
  MQTT_WS_PROTOCOL:    "${MQTT_WS_PROTOCOL}",
  MQTT_USERNAME:        "${MQTT_USERNAME}",
  MQTT_PASSWORD:        "${MQTT_PASSWORD}",
  MQTT_MESSAGE_PREFIX:  "${MQTT_MESSAGE_PREFIX}",
};
EOF

# Starte NGINX
nginx -g 'daemon off;'
