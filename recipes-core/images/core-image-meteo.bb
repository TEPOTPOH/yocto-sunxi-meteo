DESCRIPTION = "Image based on Sato for meteo station with LCD display"

require recipes-core/images/core-image-base.bb

# setup timezone and updating time via NTP
IMAGE_INSTALL:append = " ntp tzdata ntp-waiter"

export IMAGE_BASENAME = "core-image-meteo"

# add application that handles CO2 sensors
IMAGE_INSTALL:append = " co2-sensor-daemon"
# add MQTT server
IMAGE_INSTALL:append = " mosquitto"
# add application that handles HTU21D relative humidity and temperature sensor
IMAGE_INSTALL:append = " htu21d-daemon"

# configuration for rust applications
set_global_env() {
    mkdir -p ${IMAGE_ROOTFS}${sysconfdir}/profile.d
    echo "export MQTT_DEVICE_NAME=${MACHINE}" > ${IMAGE_ROOTFS}${sysconfdir}/profile.d/set_global_env.sh
    echo "export SLINT_KMS_ROTATION=270" >> ${IMAGE_ROOTFS}${sysconfdir}/profile.d/set_global_env.sh
}
ROOTFS_POSTPROCESS_COMMAND += "set_global_env;"

# add GUI application
IMAGE_INSTALL:append = " slint-gui liberation-fonts"

# add weather-provider daemon
IMAGE_INSTALL:append = " weather-provider"

# other required packages
IMAGE_FEATURES += "x11 ssh-server-dropbear hwcodecs"

# wifi support
IMAGE_INSTALL:append = "${@bb.utils.contains('DISTRO_FEATURES', 'rtl8821cu', ' rtl8821cu usb-modeswitch usb-modeswitch-data rfkill', '', d)}"

WIFI_SSID ?= ""
WIFI_PASS ?= ""

wifi_config () {
    echo "network={" >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant.conf
    echo ' ssid="${WIFI_SSID}"' >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant.conf
    echo ' psk="${WIFI_PASS}"' >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant.conf
    echo "} " >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant.conf

    INTERFACES_FILE=${IMAGE_ROOTFS}${sysconfdir}/network/interfaces
    # Set autostart wirless interface at boot
    if ! grep -q "^auto wlan0" "$INTERFACES_FILE"; then
        sed -i "/^iface wlan0/i auto wlan0" $INTERFACES_FILE
    fi
    # Add waiting for driver initialization finished
    if ! grep -q "^pre-up sleep 5" "$INTERFACES_FILE"; then
        sed -i "/^iface wlan0/a pre-up sleep 5" $INTERFACES_FILE
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "${@bb.utils.contains('DISTRO_FEATURES', 'rtl8821cu', 'wifi_config;', '', d)}"
