DESCRIPTION = "Python application to read sensors data and send it to MQTT broker"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://github.com/R4scal/mhz19-mqtt-daemon;branch=master;protocol=https"
# Stop using unicode module and add setup serial device in config
SRC_URI:append = " file://0001-Stop-using-uniocode-module-add-setup-serial-device-a.patch"
# add custom config
SRC_URI:append = " file://config.ini"
# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI:append = " file://${BPN}.service"
SRC_URI:append = " file://${BPN}.init"
SRCREV = "df32605ad81cb41a5df68567d914c1e701620218"

S = "${WORKDIR}/git"

RDEPENDS:${PN} += "python3 python3-setuptools python3-core python3-mhz19 python3-paho-mqtt \
    python3-sdnotify python3-colorama"

inherit systemd update-rc.d

DAEMON_WDIR = "/opt/${BPN}"

do_install:append () {
    rm -f config.ini.dist
    install -d ${D}${DAEMON_WDIR}
    install -m 0755 *.py ${D}${DAEMON_WDIR}/
    install -m 0755 ../config.ini ${D}${DAEMON_WDIR}/

    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ../${BPN}.service ${D}${systemd_unitdir}/system/${BPN}.service
    sed -i -e 's,@DMNWORKDIR@,${DAEMON_WDIR},g' \
        -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        ${D}${systemd_unitdir}/system/${BPN}.service

    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ../${BPN}.init ${D}${sysconfdir}/init.d/${BPN}
    sed -i -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        -e 's,@SYSCONFDIR@,${sysconfdir},g' \
        -e 's,@DMNWORKDIR@,${DAEMON_WDIR},g' \
        ${D}${sysconfdir}/init.d/${BPN}
}

FILES:${PN} += "${DAEMON_WDIR} ${systemd_unitdir}/system/${BPN}.service ${sysconfdir}/init.d"

SYSTEMD_SERVICE:${PN} = "${BPN}.service"

INITSCRIPT_NAME = "${BPN}"
INITSCRIPT_PARAMS = "defaults 31"
