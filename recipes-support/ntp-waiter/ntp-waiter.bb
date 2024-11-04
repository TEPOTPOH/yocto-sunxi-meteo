SUMMARY = "Waiter for NTP synchronization"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Configure startup
SRC_URI:append = " \
    file://${BPN}.init \
"

S = "${WORKDIR}"

# TODO: systemd version?
inherit update-rc.d

setup_env_cmd = ". ${sysconfdir}/profile.d/set_global_env.sh"

do_install[vardepsexclude] = "${BPN}"

do_install:append () {
    install -d ${D}${sysconfdir}/init.d/
    install -m 0755 ${S}/${BPN}.init ${D}${sysconfdir}/init.d/${BPN}
    sed -i -e 's,@BINDIR@,${USRBINPATH},g' \
        -e 's,@LOCALSTATEDIR@,${localstatedir},g' \
        -e 's,@SYSCONFDIR@,${sysconfdir},g' \
        -e 's,@DMNWORKDIR@,${USRBINPATH},g' \
        -e 's,@SETUPENV@,${setup_env_cmd},g' \
        -e 's,@BUILDDATE@,${DATE},g' \
        ${D}${sysconfdir}/init.d/${BPN}
}

FILES:${PN} += "${sysconfdir}/init.d"

INITSCRIPT_NAME = "${BPN}"
# start before applications that use internet
INITSCRIPT_PARAMS = "defaults 30"

