SUMMARY = "Rust daemon for TE HTU21D relative humidity and temperature sensor to get from and send data to MQTT broker"
#HOMEPAGE = "https://github.com/jdeeny/htu21d-rs"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit cargo_bin

SRC_URI += "\
    file://src \
    file://Cargo.toml \
"

S = "${WORKDIR}"

do_compile[network] = "1"


# Configure startup
# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI += "file://htu21d-daemon.service"
SRC_URI += "file://htu21d-daemon.init"

inherit systemd update-rc.d

do_install:append () {
    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${S}/${PN}.service ${D}${systemd_unitdir}/system/${PN}.service
    sed -i -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        ${D}${systemd_unitdir}/system/${PN}.service

    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ${S}/${PN}.init ${D}${sysconfdir}/init.d/${PN}
    sed -i -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        -e 's,@SYSCONFDIR@,${sysconfdir},g' \
        -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        ${D}${sysconfdir}/init.d/${PN}
}

FILES:${PN} += "${systemd_unitdir}/system/${PN}.service ${sysconfdir}/init.d"

SYSTEMD_SERVICE:${PN} = "${PN}.service"

INITSCRIPT_NAME = "${PN}"
INITSCRIPT_PARAMS = "defaults 35"