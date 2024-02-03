SUMMARY = "Custom Matchbox session"

LIC_FILES_CHKSUM = "file://session;endline=3;md5=98119b16a0e90d51239ab1c870ccf6fb"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " file://session"

# TODO: move to config
app_name = "slint-gui"

do_install:append() {
	install -d ${D}/${sysconfdir}/matchbox
	rm ${D}/${sysconfdir}/matchbox/session
	install -D ${S}/session ${D}/${sysconfdir}/matchbox/session
	chmod +x ${D}/${sysconfdir}/matchbox/session
	# set gui app name
	sed -i -e 's,@APPNAME@,${app_name},g' \
        ${D}/${sysconfdir}/matchbox/session
}
