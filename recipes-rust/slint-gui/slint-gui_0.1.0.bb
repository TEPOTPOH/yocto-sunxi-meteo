SUMMARY = "GUI for meteostation based on Slint - Rust UI framework"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit cargo_bin

SRC_URI += "\
    file://build.rs \
    file://ui \
    file://src \
    file://Cargo.toml \
"

S = "${WORKDIR}"

do_compile[network] = "1"
