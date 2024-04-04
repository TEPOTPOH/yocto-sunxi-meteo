FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append:cubieboard = " file://0001-uart4.patch"
SRC_URI:append:cubieboard = " file://axp20x.cfg"
