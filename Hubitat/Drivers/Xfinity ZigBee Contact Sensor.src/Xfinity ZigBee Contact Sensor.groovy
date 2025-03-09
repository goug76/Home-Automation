/**
 *  Xfinity Contact Sensor
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
 
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.device.Protocol

public static String version()          {  return "v2.0.2"  }
public static String name()             {  return "Xfinity Contact Sensor"  }
public static String codeUrl()
{
    return "https://raw.githubusercontent.com/goug76/Home-Automation/refs/heads/master/Hubitat/Drivers/Xfinity%20ZigBee%20Contact%20Sensor.src/Xfinity%20ZigBee%20Contact%20Sensor.groovy"
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
public static Integer defaultDelay()    {  return 333  }    //default delay to use for zigbee commands (in milliseconds)
public static Double minVolt()          {  return 2.3  }    //default minimum volts to use for battery volt command
public static Double maxVolt()          {  return 3.0  }    //default maximum volts to use for battery volt command

metadata {
	definition (name: name(), namespace: "TOSH-SmartHome", author: "John Goughenour", importUrl: codeUrl()) {
        capability "Configuration"
        capability "Sensor"
        capability "ContactSensor"
        capability "Battery"
        capability "TemperatureMeasurement"
        capability "TamperAlert"
        capability "Refresh"
        
        attribute "batteryLastReplaced", "String"
        
        command "resetBatteryReplacedDate", [[name: "Date Changed", type: "DATE", description: "Enter the date the battery was last changed. If blank will use current date."]]
        
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05,FD50", outClusters:"0019", model:"LDHD2AZW", manufacturer:"Leedarson"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"URC4460BC0-X-R", manufacturer:"Universal Electronics Inc"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"MCT-350 SMA", manufacturer:"Visonic"
	}
    
    preferences {
        input(name: "batteryInterval", type: "number", title: "<b>Battery Reporting Interval</b>", defaultValue: 12, 
              description: "Set battery reporting interval by this many <b>hours</b>.</br>Default: 12 hours", required: false)
        input(name: "tempInterval", type: "number", title: "<b>Temperature Reporting Interval</b>", defaultValue: 0, 
              description: "Set temperature reporting interval by this many <b>minutes</b>. </br>Default: 0 (Disabled)", required: false)
        input name: "tempOffset", title: "<b>Temperature Calibration</b>", type: "number", range: "-128..127", defaultValue: 0, required: true, 
            description: "Adjust temperature by this many degrees.</br>Range: -128 thru 127</br>Default: 0"
        input(name: "mqttBroker", type: "string", title: "<b>MQTT Broker</b>", description: "Enter MQTT Broker IP and Port e.g. server_IP:1883", required: false)
        input(name: "mqttUsername", type: "string", title: "<b>MQTT Username</b>", description: "Enter MQTT broker username", required: false)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "Enter password for your MQTT Broker", required: false)
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A Zigbee driver for Xfinity door and window sensors. ${driverInfo()}"
  }
}

def installed(){
	if(infoLogging) log.info "${device.displayName} is installing"
    configure()
	initialize()
}

def updated(){
	if(infoLogging) log.info "${device.displayName} is updating"
    interfaces.mqtt.disconnect()
	unschedule()
    configure()
	initialize()
}

def uninstalled() {
    if(infoLogging) log.info "${device.displayName} is uninstalling"
	unschedule()
    interfaces.mqtt.disconnect()
}

def initialize(){
    if(infoLogging) log.info "${device.displayName} is  initializing"
    refresh()
}

def refresh() {
	if(infoLogging) log.info "${device.displayName} is refreshing"
    def cmd = [
        "he rattr 0x${device.deviceNetworkId} 1 0x0402 0x0000 {}","delay ${defaultDelay}",  //temp
        "he rattr 0x${device.deviceNetworkId} 1 ${zigbee.POWER_CONFIGURATION_CLUSTER} 0x0020 {}","delay ${defaultDelay()}",  //battery
    ]
    return cmd
}

def configure() {
	if(infoLogging) log.info "${device.displayName} is configuring setup"
    if(mqttBroker && mqttUsername) state.mqtt = true else state.mqtt = false
    int reportInterval = batteryInterval.toInteger() * 60 * 60
    List cmd = ["zdo bind 0x${device.deviceNetworkId} 1 1 0x0500 {${device.zigbeeId}} {}", "delay ${defaultDelay()}",] // IAS Zone
    cmd += zigbee.enrollResponse(1200) // Enroll in IAS Zone
    cmd += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020, DataType.UINT8, 0, reportInterval, 1, [:], defaultDelay()) //Battery Voltage Reporting
    cmd += zigbee.temperatureConfig(0,(tempInterval.toInteger() * 60)) // Temperature Reporting
    cmd += refresh()
    
    return cmd
}

def resetBatteryReplacedDate(date) {
    if(date)
        sendEvent(name: "batteryLastReplaced", value: date.format('yyyy-MM-dd'))
    else
        sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd'))
    if(state.mqtt) sendMqttCommand("${device.currentValue('batteryLastReplaced')}", "batteryLastReplaced")
	if(infoLogging) log.info "${device.displayName} is setting Battery Last Replaced Date ${device.currentValue('batteryLastReplaced')}"
}

def sendMqttCommand(cmnd, payload) {
    if(debugLogging) log.debug "${device.displayName} MQTT sending Command: ${cmnd} Payload: ${payload}"
    try {
        if(debugLogging) log.debug "${device.displayName} settting up MQTT Broker"
        if( !interfaces.mqtt.isConnected() ) {
            interfaces.mqtt.connect(
                "tcp://${mqttBroker}", 
                "${location.hub.name.toLowerCase().replaceAll(' ', '_')}_${device.getDeviceNetworkId()}", 
                mqttUsername, mqttPassword
            )
        }
            
        if(debugLogging) 
            log.debug "${device.displayName} is sending Topic: stat/${device.displayName.toLowerCase().replaceAll(' ', '_')}/${payload}/ Command: ${cmnd}"
        interfaces.mqtt.publish(
            "stat/${device.displayName.toLowerCase().replaceAll(' ', '_')}/${payload}/", 
            "${cmnd}", 2, true
        )                      
    } catch(Exception e) {
        log.error "${device.displayName} unable to send MQTT status ${e}"
    }
}

void parse(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)
    if(debugLogging) log.debug "${device.displayName} is parsing message: ${description}"

    if (description?.startsWith("enroll request")) {
        def result = []
        List cmds = zigbee.enrollResponse(1200)        
        if(debugLogging) log.debug "enroll response: ${cmds}"        
        result = cmds?.collect { new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
    } else if (description?.startsWith('zone status') || description?.startsWith('zone report')) {
        if(debugLogging) log.debug "Zone status: ${description}"
        parseIasMessage(description)
    } else {
        if(debugLogging) log.debug "Attribute Message: ${descMap}"
        parseAttributeMessage(description)
    }
}

private parseIasMessage(String description) {
    ZoneStatus zs = zigbee.parseZoneStatus(description)
    if(debugLogging) log.debug "${device.displayName} is parsing ISA message: ${description}"
    if(debugLogging) log.debug "${device.displayName} is parsing ISA message Tamper: ${zs.tamper} Contact: ${zs.alarm1}"

    if( zs.tamper ) {
        if(device.currentValue('tamper') != "detected") {
            if(infoLogging) log.info "${device.displayName} tamper detected"
            sendEvent(name:'tamper', value: 'detected', descriptionText: "${device.displayName} tamper detected.", type: 'physical')
            if(state.mqtt) sendMqttCommand("detected", "tamper")
        }
    } else {
        if(device.currentValue('tamper') != "clear") {
            if(infoLogging) log.info "${device.displayName} tamper cleared"
            sendEvent(name:'tamper', value: 'clear', descriptionText: "${device.displayName} tamper is cleared.", type: 'physical')
            if(state.mqtt) sendMqttCommand("clear", "tamper")
        }
    }
    if( zs.alarm1 ) {
        if(device.currentValue('contact') != "open") {
            if(infoLogging) log.info "${device.displayName} contact opened"
            sendEvent(name:'contact', value: 'open', descriptionText: "${device.displayName} is open.", type: 'physical')
            if(state.mqtt) sendMqttCommand("open", "contact")
        }
    } else {
        if(device.currentValue('contact') != "closed") {
            if(infoLogging) log.info "${device.displayName} contact closed"
            sendEvent(name:'contact', value: 'closed', descriptionText: "${device.displayName} is closed.", type: 'physical')
            if(state.mqtt) sendMqttCommand("closed", "contact")
        }
    }
}

private parseAttributeMessage(String description) {
    Map event = zigbee.getEvent(description)
    if(debugLogging) log.debug "${device.displayName} event message: ${event}"
    if(event) {
        switch(event.name) {
            case 'batteryVoltage':
                def pct = (event.value - minVolt()) / (maxVolt() - minVolt())
                def roundedPct = Math.round(pct * 100)
                if (roundedPct <= 0) roundedPct = 1
                def descriptionText = "${device.displayName} battery was ${Math.min(100, roundedPct)}%"
                if(infoLogging) log.info "${descriptionText}"
                sendEvent(name: 'battery', value: Math.min(100, roundedPct), unit: "%", descriptionText: descriptionText, type: 'physical')
                if(state.mqtt) sendMqttCommand(Math.min(100, roundedPct), "battery")
                break
            default:
                if(infoLogging) log.info "${event.descriptionText}"
                sendEvent(name: event.name, value: event.value, unit: "°${event.unit}", descriptionText: event.descriptionText, type: 'physical')
                if(state.mqtt) sendMqttCommand(event.value, event.name)        
                break
        }
    }
}

// parse events and messages
def mqttClientStatus(message) {
    if(debugLogging) log.debug "MQTT Client Status: ${message}"
    switch(message) {
        case ~/.*Connection succeeded.*/:
            if(debugLogging) 
                log.debug "MQTT Client Status: ${device.displayName} successfully connected to MQTT Broker"
            break
        case ~/.*Error.*/:
            log.error "MQTT Client Status: ${device.displayName} connection to MQTT Broker has encountered an error - ${message}"
            break
        default:
            log.warn "MQTT Client Status: ${device.displayName}: unknown status - ${message}"
    }
}