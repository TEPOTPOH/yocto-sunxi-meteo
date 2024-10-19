SUMMARY = "Featch current weather and forecast and send data to MQTT broker"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit cargo_bin

DEPENDS += "openssl"

SRCREV = "c9ad353b0ac35203bf38dfa18486fb927da218d0"
SRC_URI:append = " \
    git://github.com/TEPOTPOH/mqtt-weather-provider.git;branch=main;protocol=https \
"
# Configure startup
# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI:append = " file://${BPN}.service"
SRC_URI:append = " file://${BPN}.init"

S = "${WORKDIR}/git"

do_compile[network] = "1"

inherit systemd update-rc.d

setup_env_cmd = ". ${sysconfdir}/profile.d/set_global_env.sh"

do_install:append () {
    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/${BPN}.service ${D}${systemd_unitdir}/system/${BPN}.service
    sed -i -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        ${D}${systemd_unitdir}/system/${BPN}.service

    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ${WORKDIR}/${BPN}.init ${D}${sysconfdir}/init.d/${BPN}
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
INITSCRIPT_PARAMS = "defaults 55"
