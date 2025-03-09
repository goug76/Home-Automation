# Virtual Hybrid Contact Switch
This is a vitrual switch for the Hubitat Elevation platform that can be used with other platforms that don't support switches as a trigger event (such as Amazon Alexa).  When you toggle the switch on, it will open the contact sensor and vice versa.

## FEATURES
    - The ability to set an auto off timer in seconds
    - Publishes an MQTT topic when turned on/off for easy integration with other platforms like Home Assistant
    - Custom commands to send MQTT topic from Rules Machine Without having to control the switch

## Installation
You can find detailed step by step instruction at [This Old Smart Home](https://thisoldsmarthome.com/automations/alexa-speaks-shared-devices/?tab=hubitat).

### MQTT
Easily integrates with MQTT, just add your broker information under preferences and click save. The topic is automatically configured and uses the devices display name in lowercase and an '_' replaces the spaces. 
#### Topics
    - cmnd/device_name