DESCRIPTION = "Image based on core-image-base for meteo station with LCD display"

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

# configuration for applications
OUTDOOR_VIDEO_URI ?= "using-default-app-video-url"
set_global_env() {
    mkdir -p ${IMAGE_ROOTFS}${sysconfdir}/profile.d
    GLOBAL_ENV_FILE=${IMAGE_ROOTFS}${sysconfdir}/profile.d/set_global_env.sh

    echo "export MQTT_DEVICE_NAME=${MACHINE}" > $GLOBAL_ENV_FILE

    # https://releases.slint.dev/1.8.0/docs/slint/src/advanced/backend_linuxkms#display-rotation
    echo "export SLINT_KMS_ROTATION=270" >> $GLOBAL_ENV_FILE

    if [ "${@bb.utils.contains('OUTDOOR_VIDEO_URI', 'using-default-app-video-url', '0', '1', d)}" = "1" ] ; then
		echo "export VIDEO_URL=${OUTDOOR_VIDEO_URI}" >> $GLOBAL_ENV_FILE
	fi
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
    echo ' psk=${WIFI_PASS}' >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant.conf
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

# https://releases.slint.dev/1.8.0/docs/slint/src/advanced/backend_linuxkms#display-rotation
touch_rotate () {
    echo 'ENV{LIBINPUT_CALIBRATION_MATRIX}="0 -1 1 1 0 0" # 90 degree clockwise' > ${IMAGE_ROOTFS}${sysconfdir}/udev/rules.d/libinput.rules
}
ROOTFS_POSTPROCESS_COMMAND += "touch_rotate;"
