/**
 *  Virtual Hybrid Contact Switch
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

public static String version()      {  return "v2.0.3"  }
public static String name()         {  return "Virtual Hybrid Contact Switch"  }
public static String codeUrl()
{
    return "https://raw.githubusercontent.com/goug76/Home-Automation/refs/heads/master/Hubitat/Drivers/Virtual%20Hybrid%20Contact%20Switch.src/Virtual%20Hybrid%20Contact%20Switch.groovy"
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

metadata 
{
    definition (name: name(), namespace: "tosh", author: "John Goughenour", importUrl: codeUrl()) 
    {
        capability "Sensor"
        capability "Contact Sensor"
        capability "Switch"
    
        command "sendMqttOn"
        command "sendMqttOff"
    }
    preferences 
    {
        input(name: "mqttBroker", type: "string", title: "<b>MQTT Broker</b>", description: "Enter MQTT Broker IP and Port e.g. server_IP:1883", required: false)
        input(name: "mqttUsername", type: "string", title: "<b>MQTT Username</b>", description: "Enter MQTT broker username", required: false)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "Enter password for your MQTT Broker", required: false)
        input name: "autoOff", type: "number", title: "<b>Auto Off</b>", 
            description: "Automatically turn off device after x many seconds </br>Default: 0 (Disabled)",
            defaultValue: 0, required: false
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "false", description: "", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A hybrid contact sensor/swtich that can be used in Hubitat to trigger Alexa Routines. ${driverInfo()}"
    }
}

def installed()
{
	if(infoLogging) log.info "${device.displayName} is installing"
	initialize()
}

def updated()
{
	if(infoLogging) log.info "${device.displayName} is updating"
	initialize()
}

def uninstalled()
{
	if(infoLogging) log.info "${device.displayName} is uninstalling"
}

def initialize()
{
    if(infoLogging) log.info "${device.displayName} is initializing"
    if(mqttBroker && mqttUsername) state.mqtt = true 
    else state.mqtt = false
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "contact", value: "closed")  
}

// handle commands
def on()
{
    if(infoLogging) log.info "${device.displayName} is turned on"
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "contact", value: "open")
    if(state.mqtt) sendMqttCommand("on")
    if( autoOff > 0 ) runIn (autoOff, off)
}

def off()
{
    if(infoLogging) log.info "${device.displayName} is turned off"
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "contact", value: "closed")
    if(state.mqtt) sendMqttCommand("off")
}

def sendMqttOn()
{
    if(infoLogging) log.info "${device.displayName} sendMqttOn was triggered"
    if(state.mqtt) sendMqttCommand("on")
}

def sendMqttOff()
{
    if(infoLogging) log.info "${device.displayName} sendMqttOff was triggered"
    if(state.mqtt) sendMqttCommand("off")
}

def sendMqttCommand(cmnd)
{
    if(debugLogging) log.debug "${device.displayName} MQTT sending Command: ${cmnd}"
    try {
        if(debugLogging) log.debug "${device.displayName} settting up MQTT Broker"
        interfaces.mqtt.connect(
            "tcp://${mqttBroker}", 
            "${location.hub.name.toLowerCase().replaceAll(' ', '_')}_${device.getDeviceNetworkId()}", 
            mqttUsername, mqttPassword
        )
        
        if(debugLogging) 
            log.debug "${device.displayName} is sending Topic: cmnd/${device.displayName.toLowerCase().replaceAll(' ', '_')} Command: ${cmnd}"
        interfaces.mqtt.publish(
            "cmnd/${device.displayName.toLowerCase().replaceAll(' ', '_')}", 
            "${cmnd}", 2, false
        )
    } catch(Exception e) {
        log.error "${device.displayName} unable to connect to the MQTT Broker ${e}"
    }
    interfaces.mqtt.disconnect()
}

// parse events and messages
def mqttClientStatus(message)
{
    switch(message) {
        case ~/.*Connection succeeded.*/:
            if(debugLogging) 
                log.debug "MQTT Client Status: ${device.displayName} successfully connected to MQTT Broker"
            break
        case ~/.*Error.*/:
            log.error "MQTT Client Status: ${device.displayName} unable to connect to MQTT Broker - ${message}"
            break
        default:
            log.warn "MQTT Client Status: ${device.displayName}: unknown status - ${message}"
    }
}