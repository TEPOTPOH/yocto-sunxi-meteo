DESCRIPTION = "Python application to read sensors data and send it to MQTT broker"
SECTION = "examples"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

RDEPENDS:${PN} += "python3 python3-setuptools python3-core \
                 python3-mhz19 python3-paho-mqtt python3-sdnotify python3-colorama"

SRC_URI = "git://github.com/R4scal/mhz19-mqtt-daemon;branch=master;protocol=https"
SRCREV= "df32605ad81cb41a5df68567d914c1e701620218"

# Stop using unicode module and add setup serial device in config
SRC_URI += "file://0001-Stop-using-uniocode-module-add-setup-serial-device-a.patch"

# add custom config
SRC_URI += "file://config.ini"

# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI += "file://co2-sensor-daemon.service"
SRC_URI += "file://co2-sensor-daemon.init"

inherit systemd update-rc.d

S = "${WORKDIR}/git"

DAEMON_WDIR = "/opt/co2-sensor-daemon"

do_install:append () {
    rm -f config.ini.dist
    install -d ${D}${DAEMON_WDIR}
    install -m 0755 *.py ${D}${DAEMON_WDIR}/
    install -m 0755 ../config.ini ${D}${DAEMON_WDIR}/

    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ../co2-sensor-daemon.service ${D}${systemd_unitdir}/system/co2-sensor-daemon.service
    sed -i -e 's,@DMNWORKDIR@,${DAEMON_WDIR},g' \
        -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        ${D}${systemd_unitdir}/system/co2-sensor-daemon.service

    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ../co2-sensor-daemon.init ${D}${sysconfdir}/init.d/co2-sensor-daemon
    sed -i -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        -e 's,@SYSCONFDIR@,${sysconfdir},g' \
        -e 's,@DMNWORKDIR@,${DAEMON_WDIR},g' \
        ${D}${sysconfdir}/init.d/co2-sensor-daemon
}

FILES:${PN} += "${DAEMON_WDIR} ${systemd_unitdir}/system/co2-sensor-daemon.service ${sysconfdir}/init.d"

SYSTEMD_SERVICE:${PN} = "co2-sensor-daemon.service"

INITSCRIPT_NAME = "co2-sensor-daemon"
INITSCRIPT_PARAMS = "defaults 31"