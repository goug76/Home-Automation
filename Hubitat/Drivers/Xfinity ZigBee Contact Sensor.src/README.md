# Xfinity Contact Sensor
The Xfinity Contact Sensor serves as a ZigBee driver compatible with all three sensor versions available on the Hubitat Elevation platform. While initially designed for the Xfinity Home Security System, these sensors, thanks to their ZigBee technology, can be effectively utilized with most ZigBee hubs. If you're looking to purchase them in bulk, you can often find these sensors on eBay at an affordable price when buying them in packs of ten, typically priced at around $50, which averages out to just $5 each.

## FEATURES
    - Contact sensor open/closed
    - Tamper alert when opening the battery compartment
    - Measures temperature
    - Reports battery %
    - Custom attribute for last battery changed to track how long batteries last
    - Supports MQTT publishing of all 5 readings

## Installation
Detailed instructions coming soon.

### MQTT
Easily integrates with MQTT, just add your broker information under preferences and click save. The topics are automatically configured and uses the devices display name in lowercase and an '_' replaces the spaces. Below are all five topics below.
#### Topics
    - stat/device_name/contact
    - stat/device_name/tamper
    - stat/device_name/temperature
    - stat/device_name/battery
    - stat/device_name/batteryLastReplaced