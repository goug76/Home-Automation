/**
 *  LG Netcast TV
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
 *                    Initial Release
 *
 **********************************************************************************************************************/

public static String version()      {  return "v1.0.0"  }
public static String name()         {  return "LG Netcast TV"  }

metadata {
	definition (name: name(), namespace: "goug76", author: "John Goughenour", cstHandler: true) {
		capability "Audio Volume"
		capability "Switch"
		capability "TV"
        capability "Refresh"
        capability "Configuration"
        
        attribute "session", "string"
        attribute "maxVolume", "number"
        attribute "minVolume", "number"
        attribute "inputSource", "string"
        attribute "inputLabel", "string"
        attribute "majorChannel", "string"
        attribute "minorChannel", "string"
        attribute "channelName", "string"
        attribute "currentProgram", "string"
        attribute "model", "string"
        
        command "showKey"
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
        input (name: "pairingKey", type: "text", title: "<b>Pairing Key</b>", defaultValue: "", required: true)
        input ("heartbeat", "enum", title: "<b>Heartbeat</b>", options: poll, defaultValue: 60, description: "Set polling intervals from every 5 seconds to every 10 minutes<br><i>Default: </i><font color=\"red\">Every 1 minute</font>")
        input(name: "detailedLog", type: "bool", title: "<b>Detailed Logging</b>", defaultValue: "false", description: "During normal opperation the ${name()} will only report status changes (On/Off) in the event log. For more detailed debug logging turn on 'Detailed Logging'", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", description: "<p style=\"text-align:center\"><strong>John Goughenour</strong> (goug76)</br>${name()}<br/><em>${version()}</em></p>"
    }
}

def installed(){
    device.updateSetting("pairingKey",[type:"text", value:getDataValue("pairingKey")])
    sendEvent(name: "model", value: getDataValue("model"))
	initialize()
}

def updated(){
	unschedule()
	initialize()
}

def initialize(){
    def cron = ""
    update() 
    if(heartbeat.toInteger() < 60) {
        cron = "0/$heartbeat * * * * ? *"
    } else {
        cron = "0 0/${heartbeat.toInteger() / 60} * * * ? *" 
    }
    if (detailedLog) log.debug "Cron: $cron"
    schedule(cron, refresh)
    refresh()
}

def update() {
    if (detailedLog) log.debug "Executing 'update'"
    state.host = "${getDataValue("ip")}:${getDataValue("port")}"
    if (detailedLog) log.debug "Pairing Key: ${pairingKey}"
    if(pairingKey && pairingKey != getDataValue("pairingKey")) {
        updateDataValue("pairingKey", pairingKey)
    }
}

// handle commands

def mute() {
	if (detailedLog) log.debug "Executing 'mute'"
    if(device.currentValue("mute") == 'unmuted') {
        sendCommand(26)
        refresh()
    }
}

def unmute() {
    if (detailedLog) log.debug "Executing 'unmute'"
    if(device.currentValue("mute") == 'muted') {
        sendCommand(26)
        refresh()
    }
}

def setVolume(vol) {
	if (detailedLog) log.debug "Executing 'setVolume'"
    if(vol >= device.currentValue("minVolume") && vol <= device.currentValue("maxVolume")) {
	    getVolume()
        def curVol = device.currentValue("volume")
        if (detailedLog) log.debug "Current Volume: $curVol"
        if(curVol > vol) {
            for(i = vol; i != curVol; i++) {
                sendCommand(25)
                pauseExecution(1000)
            }
        } else {
             for(i = curVol; i != vol; i++) {
                 sendCommand(24)
                 pauseExecution(1000)
            }   
        }
        getVolume()
    }
}

def volumeUp() {
	if (detailedLog) log.debug "Executing 'volumeUp'"
    if(device.currentValue("volume") < device.currentValue("maxVolume")) {
	    sendCommand(24)
        refresh()
    }
}

def volumeDown() {
	if (detailedLog) log.debug "Executing 'volumeDown'"
    if(device.currentValue("volume") > device.currentValue("minVolume")) {
	    sendCommand(25)
        refresh()
    }
}

def on() {
	log.debug "Executing 'on'"
	// TV does not support power on over Ethernet
}

def off() {
	log.debug "Executing 'off'"
	sendCommand(1)
    sendEvent(name: "switch", value: "off")
}

def channelUp() {
	if (detailedLog) log.debug "Executing 'channelUp'"
	sendCommand(27)
    refresh()
}

def channelDown() {
	if (detailedLog) log.debug "Executing 'channelDown'"
	sendCommand(28)
    refresh()
}

def refresh() {
	if (detailedLog) log.debug "Executing 'refresh'"
    ping()
    if(device.currentSwitch == 'on') {
        getSession()
        getVolume()
        getChannel()
    }
}

def showKey() {
    if (detailedLog) log.debug "Executing 'showKey'"
    parent.requestPairingKey()
}

def configure() {
    if (detailedLog) log.debug "Executing 'configure'"
    parent.ssdpDiscover()
}

def sync(ip, port) {
    if (detailedLog) log.debug "$parent.label sent sync command to $device.displayName"
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port").toInteger()
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
    update()
}

def getSession() {
	if (detailedLog) log.debug "Getting session"
    def key
    if(!pairingKey) key = getDataValue("pairingKey") else key = pairingKey
    def command = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthReq</type><value>$key</value></auth>"
    sendAction("POST", "/roap/api/auth", command)
}

def getVolume() {
    if (detailedLog) log.debug "Requesting Volume Info" 
    sendAction("GET", "/udap/api/data?target=volume_info")
}

def getChannel() {
    if (detailedLog) log.debug "Requesting Current Channel Info" 
    sendAction("GET", "/udap/api/data?target=cur_channel")
}

def sendCommand(cmd) {
    if (detailedLog) log.debug "Sending Commands" 
    def command = "<?xml version=\"1.0\" encoding=\"utf-8\"?><command><session>${device.currentValue('session')}</session><type>HandleKeyInput</type><value>${cmd}</value></command>"
    sendAction("POST", "/udap/api/command", command)
}

def sendAction(method, path, body = "") { 
    if (detailedLog) log.debug "Sending Action to HUB" 
    def headers = [:]
    headers.put("HOST", "${state.host}")
    headers.put("Content-Type", "application/atom+xml")
    def httpRequest = [
    	method:		method,
        path: 		path,
        body:		body,
        headers:	headers
 	]
    try {
        def hubAction = new hubitat.device.HubAction(httpRequest, String dni = null, [callback: callBackHandler])
        if (detailedLog) log.debug "hub action: $hubAction"
        if(device.currentSwitch == 'on') {
            sendHubCommand(hubAction)
        }
    } catch (Exception e) {
    	if (detailedLog) log.debug "Exception Occured $e on $hubAction"
    }
}

void callBackHandler(hubitat.device.HubResponse hubResponse) {
    if (detailedLog) log.debug "Parsing '${hubResponse.body}'"
    if(hubResponse.status == 200) {
        def msg = new XmlSlurper().parseText(hubResponse.body)
        if(msg.session.text()) {
            sendEvent(name:'session', value:msg.session)   
        } else if(msg.data.level.text()) {
            sendEvent(name:'volume', value: msg.data.level)
            sendEvent(name:'maxVolume', value: msg.data.maxLevel)
            sendEvent(name:'minVolume', value: msg.data.minLevel)
            def isMute = msg.data.mute.toBoolean()
            if(!isMute) sendEvent(name:'mute', value: 'unmuted') else sendEvent(name:'mute', value: 'muted') 
        } else if(msg.data.physicalNum.text()) {
            sendEvent(name:'channel', value: msg.data.physicalNum)
            sendEvent(name:'inputSource', value: msg.data.inputSourceName)
            sendEvent(name:'inputLabel', value: msg.data.labelName)
            sendEvent(name:'majorChannel', value: msg.data.major)
            sendEvent(name:'minorChannel', value: msg.data.minor)
            sendEvent(name:'channelName', value: msg.data.chname)
            sendEvent(name:'currentProgram', value: msg.data.progName)
        }
    } else {
        refresh()
    }
}

def ping() {
	if (detailedLog) log.debug "Checking if device is alive'"
    asynchttpGet("pingHandler", [
		uri: "http://${getDataValue("ip")}/"	
	]);
}

def pingHandler(response, data) {
    if (detailedLog) log.debug "Error: ${response.errorMessage}"
    if(response.errorMessage.contains("Host unreachable") || response.errorMessage.contains("Connection timed out")) {
        if(device.currentValue("switch") == 'on') {
            log.debug "${device.label} was turned off"
            sendEvent(name: "switch", value: "off")
        }
    }else {
        if(device.currentValue("switch") == 'off' || !device.currentValue("switch")) {
            log.debug "${device.label} was turned on"
            sendEvent(name: "switch", value: "on")
        }
    }
}
