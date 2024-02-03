DESCRIPTION = "Image based on Sato for meteo station with display."

require recipes-sato/images/core-image-sato.bb

IMAGE_INSTALL:append = " i2c-tools ffmpeg v4l-utils nano bc apt"

DISTRO_FEATURES_DEFAULT:remove = " bluetooth 3g nfc"

# add video driver modesetting
#XSERVER:append = " xf86-video-modesetting"
# XSERVER += "xf86-video-modesetting \
#            "
# packagegroup-core-x11-xserver

export IMAGE_BASENAME = "core-image-meteo"

# add application that handles CO2 sensors
IMAGE_INSTALL:append = " python3 python3-co2-sensor-daemon"
# add MQTT server
IMAGE_INSTALL:append = " mosquitto"
# add application that handles HTU21D relative humidity and temperature sensor
IMAGE_INSTALL:append = " htu21d-daemon"

# configuration for rust applications
set_global_env(){
    mkdir -p ${IMAGE_ROOTFS}/etc/profile.d
    echo "export MQTT_DEVICE_NAME=${MACHINE}" > ${IMAGE_ROOTFS}/etc/profile.d/set_global_env.sh
}

ROOTFS_POSTPROCESS_COMMAND += "set_global_env;"

IMAGE_INSTALL:append = " slint-gui"

