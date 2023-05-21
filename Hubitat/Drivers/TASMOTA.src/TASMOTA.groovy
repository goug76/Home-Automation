/**
 *  TASMOTA
 *
 *  Copyright 2019 John Goughenour
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
 *    Version 2.0.0
 *        Added MQTT support for real time updates.
 *
 **********************************************************************************************************************/

public static String version()          {  return "v2.0.0"  }
public static String name()             {  return "TASMOTA - Basic"  }
public static String driverInfo()       {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }

metadata {
	definition (name: name(), namespace: "tosh", author: "John Goughenour") {
        capability "Configuration"
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Refresh"
        
        attribute "FriendlyName" , "string"
        attribute "Uptime" , "string"
        attribute "Version" , "string"
        attribute "Hostname" , "string"
        attribute "Authentication" , "enum", ["SUCCESS", "FAILED"]
        attribute "PowerOnState", "enum", ["ON", "OFF", "Previous"]
        attribute "Module", "string"
        attribute "MqttConnection", "enum", ["CONNECTED", "DISCONNECTED"]
        
        command "reboot"
	}

    main(["switch"])
    details(["switch", "reboot", "refresh"])
    
    preferences {
        input(name: "password", type: "password", title: "<b>Device Password</b>", description: "If you password protected your device</br>Enter Password", required: true)
        input(name: "mqttPassword", type: "password", title: "<b>MQTT Password</b>", description: "The password for your MQTT Broker.</br>Enter Password", required: true)
        input(name: "boot", type: "enum", title: "<b>Power on state</b>", options: ["On", "Off", "Previous"], required: false, 
              description: "Choose a state for when your device first powers on")
        input(name: "hostname", type: "bool", title: "<b>Hostname</b>", description: "Change device Hostname to match device's Friendly Name", required: false)
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input(name: "autoOff", type: "number", title: "<b>Auto Off</b>", defaultValue: 0, required: false, 
              description: "Automatically turn off device after x many seconds </br>Default: 0 (Disabled)")
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A LAN driver for devices flashed with the Tasmota firmware. ${driverInfo()}"
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
	initialize()
}

def initialize(){
    if(infoLogging) log.info "${device.displayName} is  initializing"
    updateDeviceInfo()
    pauseExecution(1000)
	if(boot && boot != device.currentValue("PowerOnState")) {
    	switch(boot) {
            case "On":
                action("PowerOnState", "1")
            	break
            case "Off":
                action("PowerOnState", "0")
            	break
            case "Previous":
                action("PowerOnState", "3")
            	break
        }
    }  
    
    if(autoOff && autoOff > 0) {
        def timer = autoOff * 10
        if (debugLogging) {log.debug "Setting Auto Off to ${timer} seconds"}
        action("PulseTime1", timer)
    } else {
    	if (debugLogging) {log.debug "Disabling Auto Off"}
       	action("PulseTime1", "0")
    }
    
    if("[" + device.label + "]" != device.currentValue("FriendlyName")) {
        def fName = device.label.replace(" ", "%20")
        action("FriendlyName", fName)
  	}
    if(hostname && "[" + device.label + "]" != device.currentValue("Hostname")) {
        def hName = device.label.replace(" ", "%20")
        action("Hostname", hName)
    }
    
    // Setup MQTT Connection
    try {
        interfaces.mqtt.connect("tcp://${getDataValue("MQTT_Broker")}", "hubitat_tasmota", getDataValue("MQTT_User"), mqttPassword)
        interfaces.mqtt.subscribe("${getDataValue("MQTT_Topic")}RESULT")
    } catch(Exception e) {
        log.error "Unable to connect to the MQTT Broker ${e}"
        sendEvent(name: "MqttConnection", value: 'DISCONNECTED')
    }
    
    if(!interfaces.mqtt.isConnected()) runEvery1Minute(updateStatus)
	
    runEvery5Minutes(updateDetails)
    //schedule("0 0 2 1/1 * ? *", updateDeviceInfo) // Run Daily
    schedule("0 0 3 ? * SAT *", reboot) // Run Weekly
}

def refresh() {
	if(infoLogging) log.info "${device.displayName} is refreshing"
    updateDetails()
    updateStatus()
}

def updateDeviceInfo() {
	if(debugLogging) log.debug "${device.displayName} device info Update"
	action("Module", "")
    updateDetails()
}

def updateDetails(){
	if(debugLogging) log.debug "${device.displayName} detailed update"
    action("status", "0")
}

def updateStatus(){
	if(debugLogging) log.debug "${device.displayName} refreshing status"
    action("power", "")
}

// handle commands
def open() {
	if(infoLogging) log.info "${device.displayName} on command received, status: switch - ${device.currentValue("switch")}"	
    action ("Power", "on")
    runIn (6, updateStatus)
}

def close() {
	if(infoLogging) log.info "${device.displayName} off command received, status: switch - ${device.currentValue("switch")}"
	action("Power", "on")
    runIn (6, updateStatus)
}

def reboot() {
	if(infoLogging) log.info "${device.displayName} reboot command received, current uptime - ${device.currentValue("Uptime")}"
    action("Restart", 1)
    runIn (5, updateDeviceInfo)
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

def getPowerState(num) {
	def powerState
	switch(num) {
    	case 0: powerState = "OFF"; break;
        case 1: powerState = "ON"; break;
        case 3: powerState = "Previous"; break;
    }
    if(debugLogging) log.debug "${device.displayName} getting power state: ${powerState}"
    return powerState
}

// parse events into attributes
def parse(message) {
    def msg = interfaces.mqtt.parseMessage(message)
	if(debugLogging) log.debug "Parsing '${msg}'"
	// TODO: handle 'switch' attribute
    switch(msg.payload) {
        case ~/.*POWER.*/:            
            if(msg.payload.contains("ON")) sendEvent(name: "switch", value: "on") else sendEvent(name: "switch", value: "off")
            break
        case ~/.*Action.*/:
            log.error "Parser Action"
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
    }
    else {
    	def reportedState
    	def msg = new groovy.json.JsonSlurper().parseText(hubResponse.body)
        if(debugLogging) log.debug "${device.displayName} responded with '${msg}'"
    	sendEvent(name: "Authentication", value: 'SUCCESS')        
        switch(msg) {
        	case ~/\{POWER=.*/:
            	reportedState = msg.POWER.toLowerCase()
          		break
         	case ~/\{Module=.*/:
                sendEvent(name: "Module", value: msg.Module)
            	break
          	case ~/\{Status=.*/:
            	def SonoffPower = msg.Status.Power
                def time = msg.StatusPRM.Uptime.split('T')
                def SonoffUptime = time[0] + " days " + time[1]
            	def powerState = getPowerState(msg.Status.PowerOnState)
                sendEvent(name: "PowerOnState", value: "$powerState")
                sendEvent(name: "Uptime", value: "$SonoffUptime")
                sendEvent(name: "FriendlyName", value: msg.Status.FriendlyName)
                sendEvent(name: "Version", value: msg.StatusFWR.Version)
                sendEvent(name: "Hostname", value: msg.StatusNET.Hostname)   
                if (SonoffPower == 1) {reportedState = "on"} else {reportedState = "off"}
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
                }
            	break 
        	case ~/\{PowerOnState=.*/:
            	def currentBoot = device.currentValue("PowerOnState")
                def powerState = getPowerState(msg.PowerOnState)
                if(debugLogging) log.debug "${device.displayName} updated: PowerOnState from: ${currentBoot} to ${powerState}"
            	sendEvent(name: "PowerOnState", value: powerState)
                break
          	case ~/\{Hostname=.*/:
            	def currentHostname = device.currentValue("Hostname")
            if(debugLogging) log.debug "${device.displayName} updated: Hostname from: ${currentHostname} to ${msg.Hostname}"
            	sendEvent(name: "Hostname", value: msg.Hostname)
                break
          	case ~/\{FriendlyName1=.*/:
            	def currentFriendly = device.currentValue("FriendlyName")
            if(debugLogging) log.debug "${device.displayName} updated: FriendlyName from: ${currentFriendly} to ${msg.FriendlyName1}"
            	sendEvent(name: "FriendlyName", value: msg.FriendlyName1)
                break   
        }
        def currentState = device.currentValue("switch")
        sendEvent(name: "refresh", value: "active")
        if (reportedState) sendEvent(name: "switch", value: reportedState)
        def SwitchState = device.currentValue("switch")
        if ((SwitchState != currentState) && (SwitchState)) {log.debug "$device.displayName at $device.deviceNetworkId turned $SwitchState"}
  	}    
}
