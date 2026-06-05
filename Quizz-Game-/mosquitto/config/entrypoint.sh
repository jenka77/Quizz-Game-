#!/bin/sh
set -eu

# Always (re)generate passwordfile from current env.
# Otherwise, old credentials from previous runs can remain and cause:
# CONNECTION_REFUSED_NOT_AUTHORIZED

if [ -z "${MQTT_USERNAME:-}" ] || [ -z "${MQTT_PASSWORD:-}" ]; then
  echo "ERROR: MQTT_USERNAME / MQTT_PASSWORD env vars missing."
  exit 1
fi

echo "Generating password file (overwrite)..."
echo "Credentials: ${MQTT_USERNAME} / (hidden)"

# -c overwrites the file
mosquitto_passwd -c -b /mosquitto/config/passwordfile "${MQTT_USERNAME}" "${MQTT_PASSWORD}"
# Mosquitto needs to read this file; keep it readable but not writable by others.
chown root:root /mosquitto/config/passwordfile 2>/dev/null || true
chmod 644 /mosquitto/config/passwordfile || true

echo "Starting Mosquitto..."
exec mosquitto -c /mosquitto/config/mosquitto.conf
