SUMMARY = "Custom Matchbox session"

LIC_FILES_CHKSUM = "file://session;endline=3;md5=98119b16a0e90d51239ab1c870ccf6fb"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://session"

# TODO: move to config
APPNAME = "slint-gui"

# Don't use "linuxkms" backend because it must be used without any windowing system
# https://slint.dev/releases/1.4.1/docs/slint/src/advanced/backend_linuxkms
# (or you should switch off current windowing system before run slint application with a such backend)
BACKEND_TYPE = "winit-femtovg"

do_install:append() {
    install -d ${D}/${sysconfdir}/matchbox
    rm ${D}/${sysconfdir}/matchbox/session
    install -D ${S}/session ${D}/${sysconfdir}/matchbox/session
    chmod +x ${D}/${sysconfdir}/matchbox/session
    # set gui app name
    sed -i -e 's,@APPNAME@,${APPNAME},g' \
        -e 's,@BACKEND_TYPE@,${BACKEND_TYPE},g' \
        ${D}/${sysconfdir}/matchbox/session
}
