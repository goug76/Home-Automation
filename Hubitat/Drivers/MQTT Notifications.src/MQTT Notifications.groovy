/**
 *  MQTT Notifications
 *
 *  MIT License
 *
 *  Copyright (c) 2023 This Old Smart Home
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

public static String version()      {  return "v1.0.3"  }
public static String name()         {  return "MQTT Notifications"  }
public static String codeUrl()
{
    return "https://raw.githubusercontent.com/goug76/Home-Automation/refs/heads/master/Hubitat/Drivers/MQTT%20Notifications.src/MQTT%20Notifications.groovy"
}
public static String driverInfo()
{
    return """
        <p style='text-align:center'></br>
        <strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (TOSH-SmartHome)</br>
        ${name()}</br>
        <em>${version()}</em></p>
    """
}

metadata {
    definition (name: name(), namespace: "TOSH-SmartHome", author: "John Goughenour", importUrl: codeUrl()) 
    {
        capability "Notification"
    }   
    
    preferences 
    {
        input(name: "mqttBroker", type: "string", title: "<b>MQTT Broker</b>", description: "Enter MQTT Broker IP and Port e.g. server_IP:1883", required: false)
        input(name: "mqttUsername", type: "string", title: "<b>MQTT Username</b>", description: "Enter MQTT broker username", required: false)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "Enter password for your MQTT Broker", required: false)
        input(name: "mqttTopic", type: "string", title: "<b>MQTT Topic</b>", description: "Enter MQTT Topic to publish to", required: false)
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", description: "Publish notifications to MQTT. ${driverInfo()}"
     }
}

def installed()
{
	if(infoLogging) log.info "${device.displayName} is installing"
}

def updated()
{
	if(infoLogging) log.info "${device.displayName} is updating"
}

def uninstalled()
{
	if(infoLogging) log.info "${device.displayName} is uninstalling"
}

// handle commands
def deviceNotification(message)
{
    if(infoLogging) log.info "${device.displayName} is sending notification message: ${message}"
    if(mqttBroker && mqttUsername && mqttTopic) {
        try {
            if(debugLogging) log.debug "${device.displayName} settting up MQTT Broker"
            interfaces.mqtt.connect(
                "tcp://${mqttBroker}",
                "${location.hub.name.toLowerCase().replaceAll(' ', '_')}_${device.getDeviceNetworkId()}",
                mqttUsername, mqttPassword
            )
            
            if(debugLogging) log.debug "${device.displayName} is sending Topic: ${mqttTopic} Command: ${cmnd}"
            interfaces.mqtt.publish(mqttTopic, message, 2, false)                      
        } catch(Exception e) {
            log.error "${device.displayName} unable to connect to the MQTT Broker ${e}"
        }
        interfaces.mqtt.disconnect()
    } else log.warn "${device.displayName} MQTT Broker and MQTT User are not set"
}

// parse events and messages
def mqttClientStatus(message)
{
    switch(message) {
        case ~/.*Connection succeeded.*/:
            if(debugLogging) log.debug "MQTT Client Status: ${device.displayName} successfully connected to MQTT Broker"
            break
        case ~/.*Error.*/:
            log.error "MQTT Client Status: ${device.displayName} unable to connect to MQTT Broker - ${message}"
            break
        default:
            log.warn "MQTT Client Status: ${device.displayName}: unknown status - ${message}"
    }
}