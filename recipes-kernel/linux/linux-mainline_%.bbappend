SRC_URI:append:cubieboard = " file://axp20x.cfg"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

#DTS_OVELAY_NAME ?= "sun4i-a10-uart4"
#SRC_URI:append:cubieboard = " https://github.com/armbian/sunxi-DT-overlays/blob/master/sun4i-a10/${DTS_OVELAY_NAME}.dts"
#https://github.com/armbian/sunxi-DT-overlays/blob/8e29b9245ecc17b58a875d61e4ca5671c4aca3fc/sun4i-a10/sun4i-a10-uart4.dts
#SRC_URI:append:cubieboard = " file://${DTS_OVELAY_NAME}.dts"
#KERNEL_DEVICETREE += "${DTS_OVELAY_NAME}.dtb"

SRC_URI:append:cubieboard = " file://0001-uart4.patch"

# do_configure:append() {
#     # For arm32 bit devices
#     cp ${WORKDIR}/${DTS_OVELAY_NAME}.dts ${S}/arch/${ARCH}/boot/dts
# }
