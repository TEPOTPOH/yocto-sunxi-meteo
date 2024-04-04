DESCRIPTION = "Python setuptools MH-Z19 CO2 sensor application"
SECTION = "examples"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://github.com/UedaTakeyuki/mh-z19;branch=master;protocol=https"
# Patch for delete Rpi platform dependences
SRC_URI:append = " file://0001-Deleted-Rpi-platform-dependences-and-PWM-mode.patch;patchdir=.."
SRCREV = "fd68b864460fbe1c44a3ac5af3f40d8ca9b64b26"

S = "${WORKDIR}/git/pypi"

RDEPENDS:${PN} += "python3 python3-setuptools python3-core python3-requests python3-pyserial \
    python3-configargparse python3-datetime python3-json python3-bitstruct"

inherit setuptools3
