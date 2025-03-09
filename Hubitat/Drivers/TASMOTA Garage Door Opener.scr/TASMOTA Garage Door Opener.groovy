/**
 *  TASMOTA Garage Door Opener
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

public static String version()          {  return "v1.6.0"  }
public static String name()             {  return "TASMOTA Garage Door Opener"  }
public static String codeUrl()
{
    return "https://raw.githubusercontent.com/goug76/Home-Automation/refs/heads/master/Hubitat/Drivers/TASMOTA%20Garage%20Door%20Opener.scr/TASMOTA%20Garage%20Door%20Opener.groovy"
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
    definition (name: name(), namespace: "tosh", author: "John Goughenour", importUrl: codeUrl()) {
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "GarageDoorControl"
        capability "ContactSensor"
        
        attribute "Uptime" , "string"
        attribute "Version" , "string"
        attribute "Switch" , "string"
        attribute "Authentication" , "enum", ["SUCCESS", "FAILED"]
        attribute "MqttConnection", "enum", ["CONNECTED", "DISCONNECTED", "UNKNOWN"]
        
        command "toggle"
        command "reboot"
	}
    
    preferences {
        def poll = [:]
		poll << [5 : "Every 5 seconds"]
		poll << [10 : "Every 10 seconds"]
		poll << [15 : "Every 15 seconds"]
		poll << [30 : "Every 30 seconds"]
		poll << [45 : "Every 45 seconds"]
		poll << [60 : "Every 1 minute"]
		poll << [120 : "Every 2 minutes"]
		poll << [300 : "Every 5 minutes"]
		poll << [600 : "Every 10 minutes"]

        def days = [:]
		days << ["disabled" : "Disabled"]
		days << ["SUN" : "Sunday"]
		days << ["MON" : "Monday"]
		days << ["TUE" : "Tuesday"]
		days << ["WED" : "Wednesday"]
		days << ["THU" : "Thursday"]
		days << ["FRI" : "Friday"]
		days << ["SAT" : "Saturday"]

        def hours = [:]
		hours << ["0" : "12 AM"]
		hours << ["1" : "1 AM"]
		hours << ["2" : "2 AM"]
		hours << ["3" : "3 AM"]
		hours << ["4" : "4 AM"]
		hours << ["5" : "5 AM"]
		hours << ["6" : "6 AM"]
		hours << ["7" : "7 AM"]
		hours << ["8" : "8 AM"]
		hours << ["9" : "9 AM"]
		hours << ["10" : "10 AM"]
		hours << ["11" : "11 AM"]
		hours << ["12" : "12 PM"]
		hours << ["13" : "1 PM"]
		hours << ["14" : "2 PM"]
		hours << ["15" : "3 PM"]
		hours << ["16" : "4 PM"]
		hours << ["17" : "5 PM"]
		hours << ["18" : "6 PM"]
		hours << ["19" : "7 PM"]
		hours << ["20" : "8 PM"]
		hours << ["21" : "9 PM"]
		hours << ["22" : "10 PM"]
		hours << ["23" : "11 PM"]
        
        input(name: "password", type: "password", title: "<b>Device Password</b>", 
            description: "If you password protected your device</br>Enter Password", required: true)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", 
            description: "The password for your MQTT Broker.</br>Enter Password", required: false)
        input (name: "heartbeat", type: "enum", title: "<b>Heartbeat</b>", options: poll, defaultValue: 60, 
            description: "Set polling intervals from every 5 seconds to every 10 minutes<br><i>Default: </i><font color='red'>Every 1 minute</font>")
        input (name: "rebootDay", type: "enum", title: "<b>Reboot Day</b>", options: days, defaultValue: "disabled", 
            description: "The day of the Week you would like your weekly reboot to execute<br><i>Default: </i><font color='red'>Disabled</font>")
        input (name: "rebootHour", type: "enum", title: "<b>Reboot Hour</b>", options: hours, defaultValue: 3, 
            description: "The time you would like your weekly reboot, depends on Reboot Day<br><i>Default: </i><font color='red'>3 AM</font>")
        input(name: "doorDelay", type: "number", title: "<b>Opening/Closing Delay</b>", defaultValue: 0, 
            description: "Add delay in seconds that you need before changing door status to Open/Closed<br><i>Default: 0 </i><font color='red'>Disabled</font>", required: false)
        input(name: "invertDoor", type: "bool", title: "<b>Invert Garage Door</b>", defaultValue: "false", 
            description: "If your garage door status is showing reversed you can invert it.", required: false)
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A DYI garage door opener using a Shelly 1 devices flashed with the Tasmota firmware. ${driverInfo()}"
    }
}

def installed(){
	if(infoLogging) log.info "${device.displayName} is installing"
	initialize()
}

def updated(){
	if(infoLogging) log.info "${device.displayName} is updating"
	unschedule()
	initialize()
}

def uninstalled(){
	if(infoLogging) log.info "${device.displayName} is uninstalling"
    if(getDataValue("MQTT_Topic")) interfaces.mqtt.unsubscribe("${getDataValue("MQTT_Topic")}RESULT")
    interfaces.mqtt.disconnect()
	unschedule()
}

def configure() {   
    if(infoLogging) log.info "${device.displayName} is configuring" 
    if(getDataValue("MQTT_Topic")) interfaces.mqtt.unsubscribe("${getDataValue("MQTT_Topic")}RESULT")
    interfaces.mqtt.disconnect()
    action("Module", "") //Get the module type
    action("PowerOnState", 0) // Control power state when the device is powered up
    action("setoption0", 0) // Disable save power state and use after restart
    action("PulseTime1", 10) // Set's Autooff to simulate a button press
    action("SetOption114", 1) // Detach switches from relay and send MQTT messages instead
	initialize()
}

def initialize() {
    if(infoLogging) log.info "${device.displayName} is initializing"
    if(getDataValue("MQTT_Topic")) interfaces.mqtt.unsubscribe("${getDataValue("MQTT_Topic")}RESULT")
    interfaces.mqtt.disconnect()
    sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
    refresh()
    pauseExecution(1000)
    if(invertDoor) action("switchmode1", 2) else action("switchmode1", 1)
        
    // Setup MQTT Connection
    if(state.mqtt) mqtt_connect()
    
    if(heartbeat.toInteger() < 60) {
        cron = "0/$heartbeat * * * * ? *"
    } else {
        cron = "0 0/${heartbeat.toInteger() / 60} * * * ? *" 
    }
    if (debugLogging) log.debug "${device.displayName} heartbeat cron: $cron"

    schedule(cron, refresh)
    refresh()
    if(rebootDay != "disabled") {
        if (debugLogging) log.debug "${device.displayName} will reboot weekly on ${rebootDay} at ${rebootHour}:00 hours"
        schedule("0 0 ${rebootHour} ? * ${rebootDay} *", reboot) // Run Weekly Reboot
    } 
}

def refresh() {
    if(infoLogging) log.info "${device.displayName} is refreshing"
    action("status", "0")
    if( state.mqtt && !interfaces.mqtt.isConnected() ) mqtt_connect()
}

// handle commands
def open() {
    if(device.currentValue("door") == "closed") {     
	    if(infoLogging) log.info "${device.displayName} on command received, status: switch - ${device.currentValue("switch")}"	
        action ("Power", "on")
        sendEvent(name: "door", value: "opening")
    }
}

def close() {
    if(device.currentValue("door") == "open") {
	    if(infoLogging) log.info "${device.displayName} off command received, status: switch - ${device.currentValue("switch")}"
	    action("Power", "on")
        sendEvent(name: "door", value: "closing")
    }
}

def toggle() {
	if(infoLogging) log.info "${device.displayName} toggle command received, status: switch - ${device.currentValue("switch")}"	
    action ("Power", "on")
    if(device.currentValue("door") == "closed") sendEvent(name: "door", value: "opening") else sendEvent(name: "door", value: "closing")
}

def reboot() {
	if(infoLogging) log.info "${device.displayName} reboot command received, current uptime - ${device.currentValue("Uptime")}"
    action("Restart", 1)
    runIn (5, refresh)
}

def mqtt_connect() {
    if(debugLogging) log.debug "${device.displayName} is trying to connect to MQTT Broker."
    
    if(getDataValue("MQTT_Broker") && getDataValue("MQTT_User")) {
        try {
            interfaces.mqtt.connect(
                "tcp://${getDataValue("MQTT_Broker")}", 
                "${location.hub.name.toLowerCase().replaceAll(' ', '_')}_${device.getDeviceNetworkId()}", 
                getDataValue("MQTT_User"), 
                mqttPassword)
            if(getDataValue("MQTT_Topic")) interfaces.mqtt.subscribe("${getDataValue("MQTT_Topic")}RESULT")
        } catch(Exception e) {
            log.error "Unable to connect to the MQTT Broker ${e}"
            sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
        }
    }
}

def action(cmd, payload) {
	def command = cmd
    if(payload) command += "%20${payload}"
	if (password) command += "&user=admin&password=${password}"
    if(debugLogging) log.debug "Sending command: ${command} to ${device.displayName}"
    
	def cmnd = new hubitat.device.HubAction("""GET /cm?cmnd=${command} HTTP/1.1\r\n Accept: */*\r\nHOST: ${device.deviceNetworkId}:80\r\n\r\n"""
    			, hubitat.device.Protocol.LAN, "${device.deviceNetworkId}:80", [callback: httpGetCallback])
    sendHubCommand(cmnd)
}

// parse events into attributes
def parse(message) {
    def msg = interfaces.mqtt.parseMessage(message)
	if(debugLogging) log.debug "${device.displayName} is parsing message: '${msg}'"
	// TODO: handle 'switch' attribute
    switch(msg.payload) {
        case ~/.*POWER.*/:            
            if(msg.payload.contains("ON")) sendEvent(name: "Switch", value: "on") 
            else sendEvent(name: "Switch", value: "off")
            break
        case ~/.*Action.*/:
            if(msg.payload.contains("ON")) {
                if(device.currentValue("door") == "opening" && doorDelay > 0 && !invertDoor) {
                    pauseExecution(doorDelay * 1000)
                }
                sendEvent(name: "door", value: "open")
                sendEvent(name: "contact", value: "open")
            } else {
                if(device.currentValue("door") == "closing" && doorDelay > 0 && invertDoor) {
                    pauseExecution(doorDelay * 1000)
                }
                sendEvent(name: "door", value: "closed")
                sendEvent(name: "contact", value: "closed")
            }
            break
        case "open":
            open()
            break
    }
}

def mqttClientStatus(message) {
    if(debugLogging) log.debug "MQTT Client Status: ${message}"
    switch(message) {
        case ~/.*Connection succeeded.*/:
	        if(debugLogging) log.debug "MQTT Client Status: ${device.displayName} connected to MQTT broker successfully'"
            sendEvent(name: "MqttConnection", value: 'CONNECTED')
            break
        case ~/.*Error.*/:
            log.error "MQTT Client Status: ${device.displayName} connection to MQTT Broker has encountered an error - ${message}"
            sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
            mqtt_connect()
            break
        default:
            log.warn "MQTT Client Status: ${device.displayName}: unknown status - ${message}"
            sendEvent(name: "MqttConnection", value: 'UNKNOWN')
    }
}

void httpGetCallback(hubitat.device.HubResponse hubResponse) {
	state.status = hubResponse.body
    if(debugLogging) log.debug "${device.displayName} responded with '${state.status}'"
    if (state.status.contains('WARNING')) {
        log.error "${device.displayName} (${device.deviceNetworkId}) password failure"
        sendEvent(name: "Authentication", value: 'FAILED')
    } else {
    	def reportedState
    	def msg = new groovy.json.JsonSlurper().parseText(hubResponse.body)
        if(debugLogging) log.debug "${device.displayName} responded with '${msg}'"
    	sendEvent(name: "Authentication", value: 'SUCCESS')        
        switch(msg) {
         	case ~/\{Module=.*/:
                updateDataValue("Module", "${msg.Module}")
            	break
          	case ~/\{Status=.*/:
            	if(msg.Status.Power == 0) sendEvent(name: "Switch", value: "off") else sendEvent(name: "Switch", value: "on")
                def time = msg.StatusPRM.Uptime.split('T')
                def uptime = time[0] + " days " + time[1]
                if(msg.StatusSNS.Switch1 == "ON") {
                    if(device.currentValue("door") == "opening" && doorDelay > 0 && !invertDoor) {
                        pauseExecution(doorDelay * 1000)
                    }
                    sendEvent(name: "door", value: "open")
                    sendEvent(name: "contact", value: "open")
                } else {
                    if(device.currentValue("door") == "closing" && doorDelay > 0 && invertDoor) {
                        pauseExecution(doorDelay * 1000)
                    }
                    sendEvent(name: "door", value: "closed")
                    sendEvent(name: "contact", value: "closed")
                }
                sendEvent(name: "Uptime", value: "$uptime")
                sendEvent(name: "Version", value: msg.StatusFWR.Version)
                updateDataValue("IP", msg.StatusNET.IPAddress)  
                updateDataValue("Mac", msg.StatusNET.Mac) 
                if(msg.containsKey("StatusMQT")) {
                    state.mqtt = true
                    updateDataValue("MQTT_Broker", "${msg.StatusMQT.MqttHost}:${msg.StatusMQT.MqttPort}")
                    updateDataValue("MQTT_User", msg.StatusMQT.MqttUser)  
                    updateDataValue("MQTT_Topic", "stat/${msg.Status.Topic}/")
                } else {
                    state.mqtt = false
                    removeDataValue("MQTT_Broker")
                    removeDataValue("MQTT_User")  
                    removeDataValue("MQTT_Topic")
                }
            	break
            default:
                if(debugLogging) log.debug "${device.displayName} unhandled response: '${msg}'"
                break;
        }
  	}    
}