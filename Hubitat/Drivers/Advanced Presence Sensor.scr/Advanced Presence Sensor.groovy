/**
 *  Advanced Presence Sensor
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
public static String name()             {  return "Advanced Presence Sensor"  }
public static String driverInfo()       {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }

metadata {
    definition (name: name(), namespace: "tosh", author: "John Goughenour") {
        capability "Sensor"
        capability "Presence Sensor"
        capability "Refresh"
        
        attribute "MqttConnection", "enum", ["CONNECTED", "DISCONNECTED"]
        attribute "room", "enum", ["not_home", "garage"]
        attribute "pingable", "enum", ["yes", "no"]
		
        command "arrived"
        command "departed"
        command "clearMqttSettings"
        command "setMqttBroker", [[name: "MQTT Broker*", type: "STRING", description: "Enter MQTT Broker IP and Port e.g. server_IP:1883"],
                                  [name: "MQTT User*", type: "STRING", description: "Enter MQTT broker username"]]
        command "setRoomTopic", [[name: "Room Topic", type: "STRING", description: "Enter MQTT Room Topic to listen for"]]
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
        
        input (name: "ipAddress", type: "string", title: "<b>Phone IP Address</b>", required: false, 
               description: "Enter IP address to enable Wi-F- presence. </br>Timeout is required to change presence state.")
        input (name: "timeout", type: "number", title: "<b>Timeout</b>", range: "0..99", required: false, defaultValue: 0, 
               description: "Number of tries without a response before device is not present. </br>Range: 0-99 </br>Default: 0 (Disabled)")
        input (name: "heartbeat", type: "enum", title: "<b>Heartbeat</b>", options: poll, defaultValue: 60, 
               description: "Set polling intervals from every 5 seconds to every 10 minutes </br>Used with Wi-Fi Presence </br><i>Default: </i><font color=\"red\">Every 1 minute</font>")
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "The password for your MQTT Broker.</br>Enter Password", required: false)
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "An advanced virtual presence sensor that uses MQTT to update additional attributes. ${driverInfo()}"
     }
}

def installed(){
	if(infoLogging) log.info "${device.displayName} is installing"
    initAttrib()
	initialize()
}

def updated(){
	if(infoLogging) log.info "${device.displayName} is updating"
    initAttrib()
	initialize()
}

def uninstalled(){
	if(infoLogging) log.info "${device.displayName} is uninstalling"
    interfaces.mqtt.unsubscribe("${getDataValue("MQTT_Room_Topic")}")
    interfaces.mqtt.disconnect()
}

def refresh() {
    if(infoLogging) log.info "${device.displayName} is refreshing"
    initialize()
}

def configure() {
    if(infoLogging) log.info "${device.displayName} is configuring"
    
    // Setup MQTT Connection
    if(getDataValue("MQTT_Broker") && getDataValue("MQTT_User")) {
        try {
            if(debugLogging) log.debug "${device.displayName} settting up MQTT Broker"
            interfaces.mqtt.connect("tcp://${getDataValue("MQTT_Broker")}", "hubitat_garage_door", getDataValue("MQTT_User"), mqttPassword)
            if(getDataValue("MQTT_Room_Topic")) interfaces.mqtt.subscribe("${getDataValue("MQTT_Room_Topic")}")
        } catch(Exception e) {
            log.error "Unable to connect to the MQTT Broker ${e}"
            sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
        }
    }
    
    // Setup Wi-Fi Presence
    if (ipAddress) {
        if(debugLogging) log.debug "${device.displayName} settting up Wi-Fi presence"
        state.tryCount = 0   
        if(heartbeat.toInteger() < 60) {
            cron = "0/$heartbeat * * * * ? *"
        } else {
            cron = "0 0/${heartbeat.toInteger() / 60} * * * ? *" 
        }
        schedule(cron, ping)
        runIn(2, ping)
    }
}

def initialize() {
    if(infoLogging) log.info "${device.displayName} is initializing"
	unschedule()
    configure()
}

def initAttrib() {    
    if(infoLogging) log.info "${device.displayName} is initializing attributes"
    
    // Initialize Attributes
    sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
    sendEvent(name: "room", value: 'not_home')
    sendEvent(name: "pingable", value: 'no')
}

// handle commands
def arrived() {
    if(infoLogging) log.info "${device.displayName} is present"
	sendEvent(name: "presence", value: 'present')
}

def departed() {
    if(infoLogging) log.info "${device.displayName} is not present"
	if(device.currentValue("room") == 'not_home' && device.currentValue("pingable") == 'no') sendEvent(name: "presence", value: 'not present')
}

def change_rooms(room) {
    if(infoLogging) log.info "${device.displayName} is  changing rooms"
    sendEvent(name: "room", value: room)
    if(room != 'not_home' && device.currentValue("presence") == 'not present') arrived()
}

def ping() {
    if(debugLogging) log.debug "${device.displayName} is setting checking for heartbeat"
    state.tryCount++
    
    if (timeout > 0 && state.tryCount > timeout) departed()
    
	asynchttpGet("httpGetCallback", [uri: "http://${ipAddress}/"])
}

def setMqttBroker(broker, user) {
    if(debugLogging) log.debug "${device.displayName} is setting up mqtt broker variables"
    updateDataValue("MQTT_Broker", "${broker}")
    updateDataValue("MQTT_User", user)
}

def setRoomTopic(topic) {
    if(debugLogging) log.debug "${device.displayName} is setting up mqtt room topic variable"
    if(topic) updateDataValue("MQTT_Room_Topic", "${topic}") else removeDataValue("MQTT_Room_Topic")
}

def clearMqttSettings() {
    if(debugLogging) log.debug "${device.displayName} is clearing MQTT data"
    removeDataValue("MQTT_Broker")
    removeDataValue("MQTT_User")      
}

// parse events and messages
def parse(message) {
    def msg = interfaces.mqtt.parseMessage(message)
	if(debugLogging) log.debug "Parsing '${msg}'"
	// TODO: handle 'switch' attribute
    switch(msg.topic) {
        case "${getDataValue("MQTT_Room_Topic")}":
           change_rooms(msg.payload)
           break
        default:
            if(debugLogging) log.info "Parsing ${device.displayName}: nothing to parse"
    }
}

def mqttClientStatus(message) {
    if(debugLogging) log.debug "MQTT Client Status: ${message}"
    switch(message) {
        case ~/.*Connection succeeded.*/:
            sendEvent(name: "MqttConnection", value: 'CONNECTED')
            break
        default:
            if(debugLogging) log.info "MQTT Client Status ${device.displayName}: unknown status"
    }
}

def httpGetCallback(response, data) {
	if(debugLogging) log.debug "${device.displayName}: httpGetCallback(${groovy.json.JsonOutput.toJson(response)}, data)"
	
	if (response && response.status == 408 && response.errorMessage.contains("Connection refused")) {
		state.tryCount = 0
        sendEvent(name: "pingable", value: 'yes')
		arrived()
	} else sendEvent(name: "pingable", value: 'no')
}
