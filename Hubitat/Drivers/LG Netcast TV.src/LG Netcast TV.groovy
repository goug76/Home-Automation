/**
 *  LG Netcast TV
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

public static String version()      {  return "v1.0.0"  }
public static String name()         {  return "LG Netcast TV"  }
public static String nameSpace()    {  return "tosh"  }
public static String codeUrl()
{
    return "https://raw.githubusercontent.com/goug76/Home-Automation/refs/heads/master/Hubitat/Drivers/LG%20Netcast%20TV.src/LG%20Netcast%20TV.groovy"
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
	definition (name: name(), namespace: nameSpace(), author: "John Goughenour", importUrl: codeUrl(), cstHandler: true) {
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
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "LG Netcast driver used with the Smart TV (Connect) app. ${driverInfo()}"
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
    subscribeToUPnP()
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

def subscribeToUPnP() {
    def callbackUrl = "http://${location.hub.localIP}:39501/" // Use Hubitat's local endpoint
    def headers = [:]
    headers.put("HOST", "${state.host}")
    headers.put("CALLBACK", "<${callbackUrl}>")
    headers.put("NT", "upnp:event")
    headers.put("TIMEOUT", "Second-1800") // Renew every 30 min

    def httpRequest = [
        method: "SUBSCRIBE",
        path: "/udap/api/event",  // Adjust this if needed
        headers: headers
    ]
    
    try {
        sendHubCommand(new hubitat.device.HubAction(httpRequest, null, [callback: "upnpEventHandler"]))
        if (detailedLog) log.debug "Subscribed to UPnP events"
    } catch (Exception e) {
        log.error "Failed to subscribe to UPnP events: $e"
    }
}

def upnpEventHandler(hubitat.device.HubResponse hubResponse) {
    if (detailedLog) log.debug "UPnP Event Received: ${hubResponse.body}"
    def xmlData = new XmlSlurper().parseText(hubResponse.body)
    
    if (xmlData.volume.text()) {
        sendEvent(name: "volume", value: xmlData.volume.text().toInteger())
    }
    if (xmlData.channel.text()) {
        sendEvent(name: "channel", value: xmlData.channel.text())
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
    asynchttpGet("pingHandler", [uri: "http://${getDataValue("ip")}/"])
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
