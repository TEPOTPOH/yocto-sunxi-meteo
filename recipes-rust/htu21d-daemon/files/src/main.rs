extern crate linux_embedded_hal as hal;
extern crate htu21d;
extern crate rumqttc;
extern crate json;
extern crate envconfig;

use hal::{I2cdev};
use htu21d::HTU21D;
use std::thread;
use std::time::Duration;
use rumqttc::{MqttOptions, Client, QoS};
use json::*;
use std::sync::mpsc;
use envconfig::Envconfig;

#[derive(Debug)]
#[derive(Envconfig)]
struct Config {
    #[envconfig(from = "MQTT_BROKER_HOST", default = "localhost")]
    pub mqtt_host: String,

    #[envconfig(from = "MQTT_BROKER_PORT", default = "1883")]
    pub mqtt_port: u16,

    #[envconfig(from = "MQTT_BROKER_KEEP_ALIVE", default = "5")]
    pub mqtt_keep_alive: u16,

    #[envconfig(from = "MQTT_BROKER_BASE_TOPIC", default = "homeassistant/sensor")]
    pub mqtt_base_topic: String,

    #[envconfig(from = "MQTT_DEVICE_NAME", default = "unknown")]
    pub mqtt_device_name: String,

    #[envconfig(from = "HTU21D_INTERFACE", default = "/dev/i2c-2")]
    pub dev_interface: String,

    #[envconfig(from = "HTU21D_PERIOD", default = "3")]
    pub data_send_period: u16,
}

fn main() {
    let config = Config::init_from_env().unwrap();

    println!("Using config:\n{:?}", config);

    let mut mqttoptions = MqttOptions::new("rumqtt-sync", config.mqtt_host, config.mqtt_port);
    mqttoptions.set_keep_alive(Duration::from_secs(config.mqtt_keep_alive.into()));
    println!("Connecting to MQTT broker...");
    let (mut client, mut connection) = Client::new(mqttoptions, 10);

    let (sender, receiver) = mpsc::channel();

    // publish thread
    thread::spawn(move || { loop {
        let (topic, json_payload): (String, json::JsonValue) = receiver.recv().unwrap();
        println!("publish topic {} with payload: ", topic);
        println!("{:#}", json_payload);
        client.publish(topic, QoS::AtLeastOnce, false, json_payload.dump().as_bytes()).unwrap();
        thread::sleep(Duration::from_millis(100));
    }});

    // Connection handler thread
    thread::spawn(move || {
        // The `EventLoop`/`Connection` must be regularly polled(`.next()` in case of `Connection`) in order
        // to send, receive and process packets from the broker, i.e. move ahead.
        for (_, notification) in connection.iter().enumerate() {
            println!("Notification = {:?}", notification);
        }
    });

    let sensor_name = "htu21d";
    let main_topic = config.mqtt_base_topic + "/" + &config.mqtt_device_name + "_" + sensor_name;
    let state_topic = main_topic.clone() + "/state";
    let sensor_topic = main_topic + "/" + &config.mqtt_device_name + "_" + sensor_name;
    let temp_config_topic = sensor_topic.clone() + "_temperature/config";
    let rh_config_topic = sensor_topic.clone() + "_rh/config";

    let payload = object!{
        state_topic: state_topic.clone(),
        unit_of_measurement: "°C",
        value_template: "{{ value_json.temperature }}",
        name: "HTU21D temperature",
        device_class: "temperature"
    };
    sender.send((temp_config_topic, payload)).unwrap();

    // RH
    let payload = object!{
        state_topic: state_topic.clone(),
        unit_of_measurement: "%",
        value_template: "{{ value_json.rh }}",
        name: "HTU21D relative humidity"
    };
    sender.send((rh_config_topic, payload)).unwrap();

    println!("Initialisation HTU21D ...");
    let i2c_bus = I2cdev::new(config.dev_interface).unwrap();
    let mut htu21d = HTU21D::new_primary(i2c_bus);
    htu21d.init().unwrap();

    loop {
        let measurements = htu21d.measure().unwrap();
        println!("Relative Humidity = {:.2} %", measurements.humidity);
        println!("Temperature = {:.2} deg C", measurements.temperature);

        let measures = object!{
            rh: measurements.humidity.round() as u8,
            temperature: measurements.temperature.round() as i8
        };

        sender.send((state_topic.clone(), measures)).unwrap();

        thread::sleep(Duration::from_secs(config.data_send_period.into()));
    }
}
