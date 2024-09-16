FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append:cubieboard = " \
    file://0001-uart4.patch   \
    file://0001-Fixed-VE-DMA-memory-pool-range.patch \
    file://axp20x.cfg         \
    file://cedar-support.cfg  \
"