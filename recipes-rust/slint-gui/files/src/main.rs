extern crate rumqttc;
extern crate json;
extern crate envconfig;

use std::thread;
use std::sync::Arc;
use std::time::Duration;
use rumqttc::{MqttOptions, Event, Incoming, Client, QoS};
use json::*;
// use std::sync::mpsc;
// use std::sync::mpsc::Receiver;
use envconfig::Envconfig;
use std::collections::HashMap;
use slint::*;

slint::include_modules!();

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
}

type GuiCallbackMap = HashMap<String, fn(Weak<AppWindow>, JsonValue) -> ()>;

fn main() {
    let config = Config::init_from_env().unwrap();
    println!("Using config:\n{:?}", config);

    let mut mqttoptions = MqttOptions::new("rumqtt-sync-gui", &config.mqtt_host, config.mqtt_port);
    mqttoptions.set_keep_alive(Duration::from_secs(config.mqtt_keep_alive.into()));
    println!("Connecting to MQTT broker...");

    let (mut client, mut connection) = Client::new(mqttoptions, 10);

    let mut topic_gui_map: GuiCallbackMap = HashMap::new();
    let full_topic = make_full_topic("htu21d", &config);
    topic_gui_map.insert(full_topic, update_indoor_t_rh);
    let full_topic = make_full_topic("mhz19", &config);
    topic_gui_map.insert(full_topic, update_indoor_co2);

    // let (sender, receiver) = mpsc::channel();

    let topic_gui_map_arc = Arc::new(topic_gui_map.clone());
    let topic_gui_map_copy = Arc::clone(&topic_gui_map_arc);

    let ui = AppWindow::new().unwrap();
    let ui_weak = ui.as_weak();

    // Connection handler thread
    let thread_join_handle = thread::spawn(move || {
        // The `EventLoop`/`Connection` must be regularly polled(`.next()` in case of `Connection`) in order
        //  to send, receive and process packets from the broker, i.e. move ahead.
        for (_, notification) in connection.iter().enumerate() {
            println!("MQTT notification = {:?}", notification);

            if let Ok(Event::Incoming(Incoming::Publish(packet))) = notification {
                // packet.payload.as_ref()

                let topic = packet.topic.clone();
                println!("topic = {:?}", topic.as_str());
                if topic_gui_map_copy.contains_key(topic.as_str()) {
                    let json_payload = String::from_utf8_lossy(&packet.payload);
                    println!("json_payload = {:?}", json_payload);
                    let payload: json::JsonValue = json::parse(json_payload.as_ref()).unwrap();
                    // sender.send((topic.clone().to_string(), payload.clone())).unwrap();

                    let new_ui_weak = ui_weak.clone();

                    match topic_gui_map_copy.get(topic.as_str()) {
                        Some(func) => {
                            let func_copy = func.clone();
                            thread::spawn(move || {
                                func_copy(new_ui_weak, payload);
                                // thread::sleep(Duration::from_millis(500));
                            });
                        },
                        None => println!("Received unknown topic"),
                    }
                }
            }
        }
    });

    for topic in topic_gui_map.keys() {
        client.subscribe(topic, QoS::AtLeastOnce).unwrap();
    }

    ui.run().unwrap();

    let res = thread_join_handle.join();
}

fn make_full_topic(sensor_name: &str, config: &Config) -> String {
    let full_topic = config.mqtt_base_topic.clone() + "/" + &config.mqtt_device_name + "_" + sensor_name + "/state";
    return full_topic;
}

fn update_indoor_t_rh(window_weak: Weak<AppWindow>, json_data: JsonValue) {
    window_weak
        .upgrade_in_event_loop(move |window| {
            // TODO: json_data.has_key("")
            let mut value = 0;
            if let Some(val) = json_data["temperature"].as_i32() {
                value = val;
            } else {
                value = 0;
            }
            window.global::<IndoorAdapter>().set_current_temp(value);
            let mut value = 0;
            if let Some(val) = json_data["rh"].as_i32() {
                value = val;
            } else {
                value = 0;
            }
            window.global::<IndoorAdapter>().set_current_rh(value);
        })
        .unwrap();
}

fn update_indoor_co2(window_weak: Weak<AppWindow>, json_data: JsonValue) {
    window_weak
    .upgrade_in_event_loop(move |window| {
        let mut value = 0;
        if let Some(val) = json_data["co2"].as_i32() {
            value = val;
        } else {
            value = 0;
        }
        window.global::<IndoorAdapter>().set_current_co2(value);
    })
    .unwrap();
}
