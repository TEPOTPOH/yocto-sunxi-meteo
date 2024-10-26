SUMMARY = "GUI for meteostation based on Slint - Rust UI framework"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit cargo_bin

# Backend and renderer features for build
BACKEND_TYPE ?= '"backend-linuxkms-noseat"'
RENDER_TYPE ?= '"renderer-femtovg"'

DEPENDS += "udev libxkbcommon libinput virtual/libgbm gstreamer1.0 gstreamer1.0-plugins-base"

SRCREV = "0c9506bb22ab50cedf83fa10552f9518f75d27f2"
SRC_URI:append = " \
    git://github.com/TEPOTPOH/slint-meteo-gui.git;branch=main;protocol=https \
"

S = "${WORKDIR}/git"

do_compile[network] = "1"

# About dependencies: https://github.com/slint-ui/slint/blob/v1.4.1/docs/building.md
# For Linux a few additional packages beyond the usual build essentials are needed for development and running apps:
# - xcb (libxcb-shape0-dev libxcb-xfixes0-dev on debian based distributions)
# - xkbcommon (libxkbcommon-dev on debian based distributions)
# - fontconfig library (libfontconfig-dev on debian based distributions)
RDEPENDS:${PN} += "libudev libxkbcommon fontconfig gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad"

do_configure:append() {
    # replace default backend and renerer
    # NOTE: Make recipe cleaning if yocto returns error on Cargo.toml with doubling " symbols
    sed -i -e 's,\"backend-winit\",${BACKEND_TYPE},g' \
        -e 's,\"renderer-femtovg\",${RENDER_TYPE},g' ${S}/Cargo.toml
}

# Configure startup
# daemon startup configs for systemd and sysvinit. Tested only config for sysvinit.
SRC_URI:append = " file://${BPN}.service"
SRC_URI:append = " file://${BPN}.init"

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
INITSCRIPT_PARAMS = "defaults 45"
