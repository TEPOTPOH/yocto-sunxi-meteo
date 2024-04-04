SUMMARY = "Rust daemon for TE HTU21D relative humidity and temperature sensor to get from and send data to MQTT broker"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit cargo_bin

SRC_URI:append = " \
    file://src \
    file://Cargo.toml \
"

S = "${WORKDIR}"

do_compile[network] = "1"

# Configure startup
# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI:append = " file://${BPN}.service"
SRC_URI:append = " file://${BPN}.init"

inherit systemd update-rc.d

setup_env_cmd = ". ${sysconfdir}/profile.d/set_global_env.sh"

do_install:append () {
    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${S}/${BPN}.service ${D}${systemd_unitdir}/system/${BPN}.service
    sed -i -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        ${D}${systemd_unitdir}/system/${BPN}.service

    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ${S}/${BPN}.init ${D}${sysconfdir}/init.d/${BPN}
    sed -i -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        -e 's,@SYSCONFDIR@,${sysconfdir},g' \
        -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        -e 's,@SETUPENV@,${setup_env_cmd},g' \
        ${D}${sysconfdir}/init.d/${BPN}
}

FILES:${PN} += "${systemd_unitdir}/system/${BPN}.service ${sysconfdir}/init.d"

SYSTEMD_SERVICE:${PN} = "${BPN}.service"

INITSCRIPT_NAME = "${BPN}"
INITSCRIPT_PARAMS = "defaults 35"
