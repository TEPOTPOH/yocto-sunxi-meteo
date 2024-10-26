FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append:cubieboard = " \
    file://0001-uart4.patch   \
    file://axp20x.cfg         \
    file://0001-Fixed-VE-DMA-memory-pool-range.patch \
    file://cedar-support.cfg  \
"