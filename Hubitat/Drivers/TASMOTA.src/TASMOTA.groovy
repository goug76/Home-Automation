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
 */
metadata {
	definition (name: "TASMOTA - Basic", namespace: "goug76", author: "John Goughenour") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Refresh"
        
        attribute "FriendlyName" , "string"
        attribute "Uptime" , "string"
        attribute "Version" , "string"
        attribute "Hostname" , "string"
        attribute "Mac" , "string"
        attribute "IP" , "string"
        attribute "Authentication" , "string"
        attribute "PowerOnState", "string"
        attribute "AutoOff", "number"
        attribute "Module", "string"
        
        command "reboot"
	}

    main(["switch"])
    details(["switch", "reboot", "refresh"])
    
    preferences {
        section ("Configure your Tasmota Device Settings"){
            input(name: "password", type: "password", description: "Enter Password", title: "Password \nIf you password protected your device", 
                required: true)
            input(name: "hostname", type: "bool", title: "Hostname \nChange device Hostname to match device's Friendly Name", 
                required: false)
            input(name: "boot", type: "enum", title: "Power on state", description: "Choose a state for when your device first powers on", 
                options: ["On", "Off", "Previous"], required: false)
            input(name: "autoOff", type: "number", title: "Auto Off \nAutomatically turn off device after x many seconds \n\nDefault: 0 (Disabled)", 
                defaultValue: 0, required: false)
          	input description: "During normal opperation the Tasmota device will only report status changes (On/Off) in the event logger. For more detailed debug logging turn on 'Detailed Logging'", 
            	type: "paragraph", element: "paragraph", title: "Logging"
        	input(name: "detailedLog", type: "bool", title: "Detailed Logging", required: false)
        }
     }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
}

def installed(){
	initialize()
}

def updated(){
	unschedule()
	initialize()
}

def initialize(){
	if (detailedLog) {log.debug "$device.displayName initializing"}
    if (!device.currentValue("AutoOff") && device.currentValue("AutoOff") != 0) {action("PulseTime", "")}
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
        if(timer != (device.currentValue("AutoOff") * 10)) {
            action("PulseTime", timer)
        }
    } else if(autoOff != (device.currentValue("AutoOff"))) {    
    	if (detailedLog) {log.debug "Disabling Auto Off"}
       	action("PulseTime", "0")
    }
    if("[" + device.label + "]" != device.currentValue("FriendlyName")) {
        def fName = device.label.replace(" ", "%20")
        action("FriendlyName", fName)
  	}
    if(hostname && "[" + device.label + "]" != device.currentValue("Hostname")) {
        def hName = device.label.replace(" ", "%20")
        action("Hostname", hName)
    }
    runIn (5, updateDeviceInfo)
	runEvery1Minute(updateStatus)
    runEvery5Minutes(updateDetails)
    schedule("0 0 2 1/1 * ? *", updateDeviceInfo) // Run Daily
    schedule("0 0 3 ? * SAT *", reboot) // Run Weekly
}

def updateDeviceInfo() {
	if (detailedLog) {log.debug "$device.displayName device info Update"}
	action("Module", "")
    updateDetails()
}

def updateDetails(){
	if (detailedLog) {log.debug "$device.displayName detailed update"}
    action("status", "0")
}

def updateStatus(){
	if (detailedLog) {log.debug "$device.displayName refreshing status"}
    action("power", "")
}

// handle commands
def on() {
	if (detailedLog) {log.debug "$device.displayName on command received, status: switch - ${device.currentValue("switch")}"}	
    action ("Power", "on")
    runIn (5, updateStatus)
}

def off() {
	if (detailedLog) {log.debug "$device.displayName off command received, status: switch - ${device.currentValue("switch")}"}
	action("Power", "off")
    runIn (5, updateStatus)
}

def refresh() {
	if (detailedLog) {log.debug "$device.displayName refresh command received"}
    updateDeviceInfo()
}

def reboot() {
	if (detailedLog) {log.debug "$device.displayName reboot command received, current uptime - ${device.currentValue("Uptime")}"}
    action("Restart", 1)
    runIn (5, updateDeviceInfo)
}

def action(cmd, payload) {
	def command = cmd
    if(payload) command += "%20${payload}"
	if (password) command += "&user=admin&password=${password}"
    
	def cmnd = new hubitat.device.HubAction("""GET /cm?cmnd=${command} HTTP/1.1\r\n Accept: */*\r\nHOST: ${device.deviceNetworkId}:80\r\n\r\n"""
    			, hubitat.device.Protocol.LAN, "${device.deviceNetworkId}:80", [callback: callBackHandler])
    sendHubCommand(cmnd)
}

def parseString(str) {
	def name = str.split('\\(') [1]
	name = name.split('\\)') [0]
	return name	
}

def getPowerState(num) {
	def powerState
	switch(num) {
    	case 0: powerState = "OFF"; break;
        case 1: powerState = "ON"; break;
        case 3: powerState = "Previous"; break;
    }
    return powerState
}

void callBackHandler(hubitat.device.HubResponse hubResponse) {
	state.status = hubResponse.body
    if (detailedLog) {log.debug "$device.displayName responded with '$state.status'"}
    if (state.status.contains('WARNING')) {
    	log.error "$device.displayName (${device.deviceNetworkId}) password failure"
        sendEvent(name: "Authentication", value: 'FAILED')
    }
    else {
    	def reportedState
    	def msg = new groovy.json.JsonSlurper().parseText(hubResponse.body)
    	sendEvent(name: "Authentication", value: 'SUCCESS')        
        switch(msg) {
        	case ~/\{POWER=.*/:
            	reportedState = msg.POWER.toLowerCase()
          		break
         	case ~/\{Module=.*/:
            	def mod = parseString(msg.Module)
                sendEvent(name: "Module", value: mod)
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
                sendEvent(name: "Mac", value: msg.StatusNET.Mac)
                sendEvent(name: "IP", value: msg.StatusNET.IPAddress)
                if (SonoffPower == 1) {reportedState = "on"} else {reportedState = "off"}
            	break 
        	case ~/\{PowerOnState=.*/:
            	def currentBoot = device.currentValue("PowerOnState")
                def powerState = getPowerState(msg.PowerOnState)
            	if (detailedLog) {log.debug "$device.displayName updated: PowerOnState from: $currentBoot to $powerState"}
            	sendEvent(name: "PowerOnState", value: powerState)
                break
        	case ~/\{PulseTime1=.*/:
            	def bTime = (msg.PulseTime1.split(' ') [0]).toInteger() / 10
                def currentOff = device.currentValue("AutoOff")
            	if (detailedLog && currentOff != bTime) {log.debug "$device.displayName updated: AutoOff from: $currentOff seconds to $bTime seconds"}
                sendEvent(name: "AutoOff", value: bTime, unit: "seconds")
            	break
          	case ~/\{Hostname=.*/:
            	def currentHostname = device.currentValue("Hostname")
            	if (detailedLog) {log.debug "$device.displayName updated: Hostname from: $currentHostname to ${msg.Hostname}"}
            	sendEvent(name: "Hostname", value: msg.Hostname)
                break
          	case ~/\{FriendlyName1=.*/:
            	def currentFriendly = device.currentValue("FriendlyName")
            	if (detailedLog) {log.debug "$device.displayName updated: FriendlyName from: $currentFriendly to ${msg.FriendlyName1}"}
            	sendEvent(name: "FriendlyName", value: msg.FriendlyName1)
                break   
        }
        def currentState = device.currentValue("switch")
        sendEvent(name: "refresh", value: "active")
        if (reportedState){sendEvent(name: "switch", value: reportedState)}
        def SwitchState = device.currentValue("switch")
        if ((SwitchState != currentState) && (SwitchState)) {log.debug "$device.displayName at $device.deviceNetworkId turned $SwitchState"}
  	}    
}
