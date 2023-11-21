# Replace config file for Mosquitto
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://mosquitto.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/mosquitto
    install -m 0644 ${WORKDIR}/mosquitto.conf \
        ${D}${sysconfdir}/mosquitto/mosquitto.conf
}

FILES:${PN}:append = " ${sysconfdir}/mosquitto/mosquitto.conf"
