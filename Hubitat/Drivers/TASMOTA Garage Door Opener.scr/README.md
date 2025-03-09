# TASMOTA Garage Door Opener
The TASMOTA Garage Door Opener driver is for the Hubitat Elevation platform. This is a DIY project that uses a Shelly 1 Wi-Fi switch flashed with TASMOTA firmware in conjunction with a reed switch, giving you the ability to control and monitor the state of your garage door.
## Features
    - Control your garage door Open/Close/Toggle
    - All in one unit
    - Door sensor Open/Opening/Close/Closing
    - Contact sensor Open/Closed
    - Weekly reboot schedule
    - Configurable heartbeat
    - Device Reboot
    - Invert garage door
    - Supports MQTT
## Installation
Detailed device setup instructions coming soon
### Driver Setup
    1. On the Hubitat Devices screen select the Add device button
    2. Select Virual
    3. In the Select device type dropdown seach for TASMOTA Garage Door Opener, select it and click Next
    4. Give your device a name and click next.
    5. Select a Room for your device and click Next
    6. Then, click View device details
    7. Click Device Info tab and click edit on the Device Network Id
    8. Enter the IP address for your device and click Save
    9. On the Preferences tab enter the device password and MQTT Password if they are set
    10. Then click Save. You should see the Authentication as SUCCESS and Mqtt Connection as Connected if all goes well
## MQTT
The Tasmota firmware support MQTT for device state changes, else you will have to rely on the drivers Heartbeat to pull state changes. The driver automatically pulls the MQTT broker and topics from the device.
