#!/usr/bin/env bash
#
# Convenience deployment script for Oracle Linux.
# Run as root (or via sudo). Idempotent — safe to re-run after a new build.
#
set -euo pipefail

APP_NAME="measurement-conversion-api"
APP_USER="measurement"
APP_HOME="/opt/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"
CONF_DIR="/etc/${APP_NAME}"
JAR_SRC="${1:-target/${APP_NAME}.jar}"

echo "==> Ensuring service user '${APP_USER}' exists"
id -u "${APP_USER}" >/dev/null 2>&1 || useradd --system --no-create-home --shell /sbin/nologin "${APP_USER}"

echo "==> Creating directories"
install -d -o "${APP_USER}" -g "${APP_USER}" -m 0750 "${APP_HOME}"
install -d -o "${APP_USER}" -g "${APP_USER}" -m 0750 "${LOG_DIR}"
install -d -o root          -g root          -m 0750 "${CONF_DIR}"

echo "==> Copying JAR to ${APP_HOME}/${APP_NAME}.jar"
install -o "${APP_USER}" -g "${APP_USER}" -m 0640 "${JAR_SRC}" "${APP_HOME}/${APP_NAME}.jar"

echo "==> Installing systemd unit"
install -o root -g root -m 0644 deployment/${APP_NAME}.service /etc/systemd/system/${APP_NAME}.service

if [[ ! -f "${CONF_DIR}/app.env" ]]; then
    echo "==> Seeding ${CONF_DIR}/app.env from example (edit before starting!)"
    install -o root -g root -m 0600 deployment/app.env.example "${CONF_DIR}/app.env"
fi

echo "==> Opening firewall port 8080/tcp (firewalld)"
if command -v firewall-cmd >/dev/null 2>&1; then
    firewall-cmd --permanent --add-port=8080/tcp || true
    firewall-cmd --reload || true
fi

echo "==> Reloading systemd and (re)starting service"
systemctl daemon-reload
systemctl enable "${APP_NAME}"
systemctl restart "${APP_NAME}"

echo "==> Status:"
systemctl --no-pager status "${APP_NAME}" || true

echo
echo "Done. Tail logs with:"
echo "    journalctl -u ${APP_NAME} -f"
echo "    tail -f ${LOG_DIR}/${APP_NAME}.log"
