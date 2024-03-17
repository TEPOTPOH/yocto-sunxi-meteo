SUMMARY = "GUI for meteostation based on Slint - Rust UI framework"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Backend and renderer features for Cargo.toml
BACKEND_TYPE = '"backend-winit"'
RENDER_TYPE = '"renderer-femtovg"'

inherit cargo_bin

SRC_URI:append = " \
    file://build.rs \
    file://ui \
    file://src \
    file://Cargo.toml \
"

S = "${WORKDIR}"

do_compile[network] = "1"

# About dependencies: https://github.com/slint-ui/slint/blob/v1.4.1/docs/building.md
# For Linux a few additional packages beyond the usual build essentials are needed for development and running apps:
# - xcb (libxcb-shape0-dev libxcb-xfixes0-dev on debian based distributions)
# - xkbcommon (libxkbcommon-dev on debian based distributions)
# - fontconfig library (libfontconfig-dev on debian based distributions)
# - (optional) Qt will be used when qmake is found in PATH
# - FFMPEG library clang libavcodec-dev libavformat-dev libavutil-dev libavfilter-dev libavdevice-dev libasound2-dev pkg-config
# xcb and xcbcommon aren't needed if you are only using backend-winit-wayland without backend-winit-x11.
RDEPENDS:${PN} += "libudev libxcb libxkbcommon libxkbcommon-x11 fontconfig ffmpeg"

do_configure:prepend() {
    sed -i -e 's,@BACKEND_TYPE@,${BACKEND_TYPE},g' \
        -e 's,@RENDER_TYPE@,${RENDER_TYPE},g' ${D}/../Cargo.toml
}
