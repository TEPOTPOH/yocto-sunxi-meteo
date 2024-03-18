# Allow dependencies on optional modules, in particular for "libxkbcommon-x11"
PACKAGES_DYNAMIC += "${PN}-x11"
RDEPENDS:${PN}:append = " ${PN}-x11"
