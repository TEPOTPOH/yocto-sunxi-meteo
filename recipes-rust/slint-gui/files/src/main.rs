extern crate rumqttc;
extern crate json;
extern crate envconfig;
extern crate chrono;

use std::thread;
use std::sync::Arc;
use std::time::Duration;
use rumqttc::{MqttOptions, Event, Incoming, Client, QoS};
use json::*;
use envconfig::Envconfig;
use std::collections::HashMap;
use slint::*;
use chrono::{NaiveDateTime};
use std::rc::Rc;

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

    #[envconfig(from = "DEFAULT_TIMEZONE_OFFSET_H", default = "3")]
    pub timezone_offset_h: i8,
}

type GuiCallbackMap = HashMap<String, fn(Weak<AppWindow>, JsonValue, Arc<Config>) -> ()>;

fn main() {
    let config = Config::init_from_env().unwrap();
    println!("Using config:\n{:?}", config);

    let mut mqttoptions = MqttOptions::new("rumqtt-sync-gui", &config.mqtt_host, config.mqtt_port);
    mqttoptions.set_keep_alive(Duration::from_secs(config.mqtt_keep_alive.into()));
    println!("Connecting to MQTT broker...");

    let (mut client, mut connection) = Client::new(mqttoptions, 10);

    let mut topic_gui_map: GuiCallbackMap = HashMap::new();
    topic_gui_map.insert(make_full_topic("htu21d", &config), update_indoor_t_rh);
    topic_gui_map.insert(make_full_topic("mhz19", &config), update_indoor_co2);
    topic_gui_map.insert(make_full_topic("nasa_kp", &config), update_space_weather_kp);

    let topic_gui_map_arc = Arc::new(topic_gui_map);
    let topic_gui_map_arc2 = topic_gui_map_arc.clone();

    let ui = AppWindow::new().unwrap();
    let ui_weak = ui.as_weak();

    let config_arc = Arc::new(config);

    // Connection handler thread
    let thread_join_handle = thread::spawn(move || {
        // The `EventLoop`/`Connection` must be regularly polled(`.next()` in case of `Connection`) in order
        //  to send, receive and process packets from the broker, i.e. move ahead.
        for (_, notification) in connection.iter().enumerate() {
            // println!("MQTT notification = {:?}", notification);

            if let Ok(Event::Incoming(Incoming::Publish(packet))) = notification {
                let topic = packet.topic.clone();
                println!("topic = {:?}", topic.as_str());
                if topic_gui_map_arc.contains_key(topic.as_str()) {
                    let json_payload = String::from_utf8_lossy(&packet.payload);
                    println!("json_payload = {:?}", json_payload);
                    let payload: json::JsonValue = json::parse(json_payload.as_ref()).unwrap();

                    match topic_gui_map_arc.get(topic.as_str()) {
                        Some(func) => {
                            func(ui_weak.clone(), payload, config_arc.clone());
                        },
                        None => println!("Received unknown topic"),
                    }
                }
            }
        }
    });

    for topic in topic_gui_map_arc2.keys() {
        client.subscribe(topic, QoS::AtLeastOnce).unwrap();
    }

    ui.run().unwrap();

    let _res = thread_join_handle.join();
}

fn make_full_topic(sensor_name: &str, config: &Config) -> String {
    let full_topic = config.mqtt_base_topic.clone() + "/" + &config.mqtt_device_name + "_" + sensor_name + "/state";
    return full_topic;
}

fn update_indoor_t_rh(window_weak: Weak<AppWindow>, json_data: JsonValue, _config: Arc<Config>) {
    window_weak.upgrade_in_event_loop(move |window| {
        // TODO: json_data.has_key("")
        let value = json_data["temperature"].as_i32().unwrap_or(0);
        window.global::<IndoorAdapter>().set_current_temp(value);
        let value = json_data["rh"].as_i32().unwrap_or(0);
        window.global::<IndoorAdapter>().set_current_rh(value);
    }).unwrap();
}

fn update_indoor_co2(window_weak: Weak<AppWindow>, json_data: JsonValue, _config: Arc<Config>) {
    window_weak.upgrade_in_event_loop(move |window| {
        let value = json_data["co2"].as_i32().unwrap_or(0);
        window.global::<IndoorAdapter>().set_current_co2(value);
    }).unwrap();
}

fn convert_datetime(input: &str, in_format: &str, out_format: &str, offset_hours: i64) -> String {
    let mut datetime = NaiveDateTime::parse_from_str(input, in_format).expect("Failed to parse datetime");
    datetime += chrono::Duration::hours(offset_hours);
    return datetime.format(out_format).to_string();
}

fn update_space_weather_kp(window_weak: Weak<AppWindow>, json_data: JsonValue, config: Arc<Config>) {
    window_weak.upgrade_in_event_loop(move |window| {
        if !json_data.is_array() {
            println!("Format of received data is invalid! Should be array of elements");
            return;
        }
        let chart_data = VecModel::default();
        for element in json_data.members() {
            if !element.is_object() {
                println!("Format of received data element is invalid! Should be object");
                continue;
            }
            chart_data.push(KpIndex {
                hour: convert_datetime(element["time_tag"].as_str().unwrap_or("00:00 01-01-2024"),
                                       "%H:%M %d-%m-%Y", "%H", config.timezone_offset_h.into()).into(),
                kp: element["kp"].as_f32().unwrap_or(0.0),
            });
        }
        window.global::<SpaceWeatherAdapter>().set_kp_index_data(Rc::new(chart_data).into());
    }).unwrap();
}
