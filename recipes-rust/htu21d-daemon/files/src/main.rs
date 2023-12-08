extern crate linux_embedded_hal as hal;
extern crate htu21d;
extern crate rumqttc;
extern crate json;

use hal::{I2cdev};
use htu21d::HTU21D;
use std::thread;
use std::time::Duration;
use rumqttc::{MqttOptions, Client, QoS};
use json::*;
use std::sync::mpsc;

fn main() {
    let mut mqttoptions = MqttOptions::new("rumqtt-sync", "localhost", 1883);
    mqttoptions.set_keep_alive(Duration::from_secs(5));
    println!("Connecting to MQTT broker...");
    let (mut client, mut connection) = Client::new(mqttoptions, 10);

    let (sender, receiver) = mpsc::channel();

    // publish thread
    thread::spawn(move || { loop {
        let (topic, json_payload): (&str, json::JsonValue) = receiver.recv().unwrap();
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

    let main_topic = "homeassistant/sensor/cubieboard_htu21d";
    let state_topic = "homeassistant/sensor/cubieboard_htu21d/state";
    let temp_config_topic = "homeassistant/sensor/cubieboard_htu21d/cubieboard_htu21d_temperature/config";
    let rh_config_topic = "homeassistant/sensor/cubieboard_htu21d/cubieboard_htu21d_rh/config";

    let payload = object!{
        state_topic: "homeassistant/sensor/cubieboard_htu21d/state",
        unit_of_measurement: "°C",
        value_template: "{{ value_json.temperature }}",
        name: "HTU21D temperature",
        device_class: "temperature"
    };
    sender.send((temp_config_topic, payload)).unwrap();

    // RH
    let payload2 = object!{
        state_topic: "homeassistant/sensor/cubieboard_htu21d/state",
        unit_of_measurement: "%",
        value_template: "{{ value_json.rh }}",
        name: "HTU21D relative humidity"
    };
    sender.send((rh_config_topic, payload2)).unwrap();

    println!("Initialisation HTU21D ...");
    let i2c_bus = I2cdev::new("/dev/i2c-2").unwrap();
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

        sender.send((state_topic, measures)).unwrap();

        thread::sleep(Duration::from_secs(1));
    }
}
