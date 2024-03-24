DESCRIPTION = "Image based on Sato for meteo station with LCD display"

require recipes-sato/images/core-image-sato.bb

# setup timezone and updating time via NTP
IMAGE_INSTALL:append = " ntp tzdata"

export IMAGE_BASENAME = "core-image-meteo"

# add application that handles CO2 sensors
IMAGE_INSTALL:append = " python3-co2-sensor-daemon"
# add MQTT server
IMAGE_INSTALL:append = " mosquitto"
# add application that handles HTU21D relative humidity and temperature sensor
IMAGE_INSTALL:append = " htu21d-daemon"

# configuration for rust applications
set_global_env() {
    mkdir -p ${IMAGE_ROOTFS}${sysconfdir}/profile.d
    echo "export MQTT_DEVICE_NAME=${MACHINE}" > ${IMAGE_ROOTFS}${sysconfdir}/profile.d/set_global_env.sh
}
ROOTFS_POSTPROCESS_COMMAND += "set_global_env;"

# add GUI application
IMAGE_INSTALL:append = " slint-gui"

# add weather-provider daemon
IMAGE_INSTALL:append = " weather-provider"
