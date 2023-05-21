/**
 *  TASMOTA Garage Door Opener
 *
 *  Copyright 2023 John Goughenour
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 ************************************************Change Record**********************************************************
 *
 *    Version 1.0.0
 *        Initial Release
 *
 **********************************************************************************************************************/

public static String version()          {  return "v1.0.0"  }
public static String name()             {  return "TASMOTA Garage Door Opener"  }
public static String driverInfo()       {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }

metadata {
    definition (name: name(), namespace: "tosh", author: "John Goughenour") {
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "GarageDoorControl"
        
        attribute "Uptime" , "string"
        attribute "Version" , "string"
        attribute "Switch" , "string"
        attribute "Authentication" , "enum", ["SUCCESS", "FAILED"]
        attribute "MqttConnection", "enum", ["CONNECTED", "DISCONNECTED"]
        command "setCmndTopic", [[name: "Command Topic", type: "STRING", description: "Enter MQTT Command Topic to listen for"]]
        
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
        
        input(name: "password", type: "password", title: "<b>Device Password</b>", description: "If you password protected your device</br>Enter Password", required: true)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "The password for your MQTT Broker.</br>Enter Password", required: false)
        input (name: "heartbeat", type: "enum", title: "<b>Heartbeat</b>", options: poll, defaultValue: 60, description: "Set polling intervals from every 5 seconds to every 10 minutes<br><i>Default: </i><font color=\"red\">Every 1 minute</font>")
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input(name: "invertDoor", type: "bool", title: "<b>Invert Garage Door</b>", defaultValue: "false", description: "If your garage door status is showing reversed you can invert it.", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A DYI garage door opener using a devices flashed with the Tasmota firmware . ${driverInfo()}"
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
    interfaces.mqtt.unsubscribe("${getDataValue("MQTT_Full_Topic")}RESULT")
    interfaces.mqtt.disconnect()
}

def configure() {   
    if(infoLogging) log.info "${device.displayName} is configuring" 
	unschedule()
    action("Module", "") //Get the module type
    action("PowerOnState", 0) // Control power state when the device is powered up
    action("setoption0", 0) // Disable save power state and use after restart
    action("PulseTime1", 10) // Set's Autooff to simulate a button press
    action("SetOption114", 1) // Detach switches from relay and send MQTT messages instead
	initialize()
}

def initialize() {
    if(infoLogging) log.info "${device.displayName} is  initializing"
    refresh()
    pauseExecution(1000)
    if(invertDoor) action("switchmode1", 2) else action("switchmode1", 1)
        
    // Setup MQTT Connection
    try {
        interfaces.mqtt.connect("tcp://${getDataValue("MQTT_Broker")}", "hubitat_garage_door", getDataValue("MQTT_User"), mqttPassword)
        interfaces.mqtt.subscribe("${getDataValue("MQTT_Topic")}RESULT")
        if(getDataValue("MQTT_Cmnd_Topic")) interfaces.mqtt.subscribe("${getDataValue("MQTT_Cmnd_Topic")}")
    } catch(Exception e) {
        log.error "Unable to connect to the MQTT Broker ${e}"
        sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
    }
    
    if(heartbeat.toInteger() < 60) {
        cron = "0/$heartbeat * * * * ? *"
    } else {
        cron = "0 0/${heartbeat.toInteger() / 60} * * * ? *" 
    }
    if (debugLogging) log.debug "Cron: $cron"
    schedule(cron, refresh)
    refresh()
    schedule("0 0 3 ? * SAT *", reboot) // Run Weekly Reboot 
}

def refresh() {
    action("status", "0")
}

// handle commands
def open() {
    if(device.currentValue("door") == "closed") {     
	    if(infoLogging) log.info "${device.displayName} on command received, status: switch - ${device.currentValue("switch")}"	
        action ("Power", "on")
        sendEvent(name: "door", value: "opening")
        runIn (6, refresh)   
    }
}

def close() {
    if(device.currentValue("door") == "open") {
	    if(infoLogging) log.info "${device.displayName} off command received, status: switch - ${device.currentValue("switch")}"
	    action("Power", "on")
        sendEvent(name: "door", value: "closing")
        runIn (6, refresh)
    }
}

def toggle() {
	if(infoLogging) log.info "${device.displayName} on command received, status: switch - ${device.currentValue("switch")}"	
    action ("Power", "on")
    if(device.currentValue("door") == "closed") sendEvent(name: "door", value: "opening") else sendEvent(name: "door", value: "closing")
    runIn (6, refresh)
}

def reboot() {
	if(infoLogging) log.info "${device.displayName} reboot command received, current uptime - ${device.currentValue("Uptime")}"
    action("Restart", 1)
    runIn (5, updateDeviceInfo)
}

def setCmndTopic(topic) {
    if(debugLogging) log.debug "${device.displayName} is setting up mqtt command topic variable"
    if(topic) updateDataValue("MQTT_Cmnd_Topic", "${topic}") else removeDataValue("MQTT_Cmnd_Topic")
}

def action(cmd, payload) {
	def command = cmd
    if(payload) command += "%20${payload}"
	if (password) command += "&user=admin&password=${password}"
    if(debugLogging) log.debug "Sending command: ${command}"
    
	def cmnd = new hubitat.device.HubAction("""GET /cm?cmnd=${command} HTTP/1.1\r\n Accept: */*\r\nHOST: ${device.deviceNetworkId}:80\r\n\r\n"""
    			, hubitat.device.Protocol.LAN, "${device.deviceNetworkId}:80", [callback: callBackHandler])
    sendHubCommand(cmnd)
}

// parse events into attributes
def parse(message) {
    def msg = interfaces.mqtt.parseMessage(message)
	if(debugLogging) log.debug "Parsing '${msg}'"
	// TODO: handle 'switch' attribute
    switch(msg.payload) {
        case ~/.*POWER.*/:            
            if(msg.payload.contains("ON")) sendEvent(name: "Switch", value: "on") else sendEvent(name: "Switch", value: "off")
            break
        case ~/.*Action.*/:
            if(msg.payload.contains("ON")) sendEvent(name: "door", value: "open") else sendEvent(name: "door", value: "closed")
            break
        case "garage":
            open()
            break
    }
}

def mqttClientStatus(message) {
    if(debugLogging) log.debug "MQTT Client Status: ${message}"
    switch(message) {
        case ~/.*Connection succeeded.*/:
            sendEvent(name: "MqttConnection", value: 'CONNECTED')
            break
    }
}

void callBackHandler(hubitat.device.HubResponse hubResponse) {
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
                if(msg.StatusSNS.Switch1 == "ON") sendEvent(name: "door", value: "open") else sendEvent(name: "door", value: "closed")
                sendEvent(name: "Uptime", value: "$uptime")
                sendEvent(name: "Version", value: msg.StatusFWR.Version)
                updateDataValue("IP", msg.StatusNET.IPAddress)  
                updateDataValue("Mac", msg.StatusNET.Mac) 
                if(msg.containsKey("StatusMQT")) {
                    updateDataValue("MQTT_Broker", "${msg.StatusMQT.MqttHost}:${msg.StatusMQT.MqttPort}")
                    updateDataValue("MQTT_User", msg.StatusMQT.MqttUser)  
                    updateDataValue("MQTT_Topic", "stat/${msg.Status.Topic}/")
                } else {
                    removeDataValue("MQTT_Broker")
                    removeDataValue("MQTT_User")  
                    removeDataValue("MQTT_Topic") 
                    removeDataValue("MQTT_Full_Topic")
                    removeDataValue("MQTT_Cmnd_Topic")
                }
            	break  
        }
  	}    
}
