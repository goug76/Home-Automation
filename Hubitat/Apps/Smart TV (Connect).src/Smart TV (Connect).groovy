/**
 *  Smart TV (Connect)
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
 */
definition(
    name: "Smart TV (Connect)",
    namespace: "goug76",
    author: "John Goughenour",
    description: "Smart app to discover and subscribe to TV events for Smart TVs.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics15-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics15-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics15-icn@2x.png")


preferences {    
    // add the search term and driver for TVs
    state.tvs = [:]
    state.tvs << ["urn:schemas-udap:service:netrcu:1":"LG Netcast TV"]
    
	page(name:"mainPage", title:"Smart TV Setup", content:"mainPage")
	page(name:"discovery", title:"Smart TV Discovery", content:"discovery", refreshTimeout:5)
	page(name:"pairing", title:"Smart TV Pairing", content:"pairing", refreshTimeout:5)
    page(name: "addDevices", title: "Add Smart TVs", content: "addDevices")
    page(name: "configureTv")
    page(name: "changeName")
    page(name: "deleteTv")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Manage your Smart Tvs", nextPage: null, uninstall: true, install: true) {
    	section("Configure"){
            input ("tvSelector", "enum", title: "Select TV Driver", required:true, options: state.tvs)
        	href "discovery", title:"Discover Smart Tvs", description:""
        }
        section("Additional Options") {
        	label title: "Name TV Group", required: false
            mode title: "Only run during specific mode(s)", required: false
        }
        section("Installed Devices"){
        	getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                href "configureTv", title:"$it.label", description:"", params: [tvId: it.deviceNetworkId]
            }
        }
    }
}

def configureTv(params){
    if(params.tvId) {
        state.currentTvId = params.tvId
        state.currentDisplayName = getChildDevice(params.tvId)?.displayName
    }
    dynamicPage(name: "configureTv", title: "Configure Smart TV created with this app", nextPage: null) {
        section {
            app.updateSetting("${state.currentTvId}_label", getChildDevice(state.currentTvId).label)
            input "${state.currentTvId}_label", "text", title:"TV Name", description: "", required: false
            href "changeName", title:"Change TV Name", description: "Edit the TV name above and click here to change it"
        }
        section {
            href "deleteTv", title:"Delete $state.currentDisplayName", description: ""
        }
    }
}

def discovery(params=[:]) {
    if(tvSelector) {
		def devices = deviceDiscovered()
		int tvRefreshCount = !state.tvRefreshCount ? 0 : state.tvRefreshCount as int
		state.tvRefreshCount = tvRefreshCount + 1
		def refreshInterval = 10
		
		def options = devices ?: []
		def numFound = options.size() ?: 0
		
		if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
			log.debug "Cleaning old device memory"
			state.devices = [:]
			state.tvRefreshCount = 0
			app.updateSetting("selectedTv", "")
		}
		
		if(!state.subscribe) {
			log.debug " Subscribing to location"
			ssdpSubscribe()
			state.subscribe = true
		}
		// Smart TV discovery request every 15 seconds
		if((tvRefreshCount % 5) == 0) {
			log.debug "Discovering devices..."
			discoverDevices()
		}
		
		//setup.xml request every 3 seconds except on discoveries
		if(((tvRefreshCount % 3) == 0) && ((tvRefreshCount % 5) != 0)) {
			verifyDevices()
		}
		
		return dynamicPage(name:"discovery", title:"Smart TV Search Started!", nextPage:"pairing", refreshInterval:refreshInterval){
            section("Please wait while we discover your ${state.tvs[tvSelector]}. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered."){
				input "selectedTvs", "enum", required:true, title:"Select Smart TV (${numFound} found)", multiple:false, options:options
			}
			section("Options") {
				href "discovery", title:"Reset list of discovered devices", description:"", params: ["reset": "true"]
			}
		}
    }
}

def pairing() {
	requestPairingKey()
    return dynamicPage(name:"pairing", title:"Smart TV Search Started!", nextPage:"addDevices"){
        section("Pairing Key request has been sent to your TV. Please enter the pairing key and click Next."){
        	input "pairingKey", "number", required:true, title:"Enter Pairing Key", multiple:false
        }
    }
}

def addDevices() {
	def devices = getDevices()
    def driver = state.tvs[tvSelector]
    def sectionText = ""
    
    def selectedtv = devices.find { it.value.mac == selectedTvs }
    d = getChildDevices()?.find { it.deviceNetworkId == selectedtv.value.mac }
    if(!d) {
        log.debug "Selected TV: ${selectedtv}"
        log.debug "Creating Smart TV with dni: ${selectedtv?.value.mac} and Driver: $driver"
        
        try {
            def newDevice = addChildDevice("goug76", "${driver}", selectedtv?.value.mac, selectedtv?.value.hub, [
                "label": selectedtv?.value?.name,
                "data": [
                    "pairingKey": pairingKey,
                    "model": selectedtv.value.model,
                    "port": selectedtv.value.port,
                    "mac": selectedtv.value.mac,
                    "ip": selectedtv.value.ip,
                ]
           ]) 
            sectionText = sectionText + "Succesfully added Smart TV with ip address ${selectedtv.value.ip} \r\n"
        } catch(e) {
            sectionText = sectionText + "An error occured ${e} \r\n"
        }
    }
    log.debug sectionText
    return dynamicPage(name:"addDevices", title:"Devices Added", nextPage:"mainPage") {
    	if(sectionText != ""){
        	section("Add Smart TV Results:") {
            	paragraph sectionText
            }
        } else {
        	section("No devices added") {
            	paragraph "All selected TVs have previously been added"
            }
        }
    }
}

def changeName() {
    def tv = getChildDevice(state.currentTvId)
    tv.label = settings["${state.currentTvId}_label"]
    dynamicPage(name: "changeName", title: "Change Name Summary", nextPage: "mainPage") {
        section {
            paragraph "${state.currentDisplayName} has been renamed to ${tv.label}. Press \"Next\" to continue"
        }
    }
}

def deleteTv() {
    try {
        unsubscribe()
        deleteChildDevice(state.currentTvId)
        dynamicPage(name: "deleteTv", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "${state.currentDisplayName} has been deleted. Press \"Next\" to continue"
            }
        }
    } catch(e) {
        dynamicPage(name: "deleteTv", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "Error: ${(e as String).split(":")[1]}. Press \"Next\" to continue"
            }
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

    unschedule()
	unsubscribe()
	initialize()
}

def uninstalled() {
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
	ssdpSubscribe()
    runEvery3Hours("ssdpDiscover")
}

Map deviceDiscovered() {
	def verfiedNetcast = getVerifiedDevices()
	def map = [:]
    verfiedNetcast.each {
    	log.debug "Discovered Smart TV Devices: $it"
        def value = "${it.value.name}"
        def key = "${it.value.mac}"
        
        map["${key}"] = value
        log.debug "Devices Discovered $map"
    }
    map
}

void verifyDevices() {
	log.debug "Verifying devices"
	def devices = getDevices().findAll { it?.value?.verified != true }
    def bandaid
    
    devices.each {
        if(!it.value.ssdpPath.contains("xml")) bandaid = "${it.value.ssdpPath}?target=netrcu.xml" else bandaid = it.value.ssdpPath
    	def ip = convertHexToIP(it.value.networkAddress)
        def port = convertHexToInt(it.value.deviceAddress)
        String host = "${ip}:${port}"
        log.debug "ssdp Path: ${bandaid} => Host: $host"
        sendHubCommand(new hubitat.device.HubAction("""GET ${bandaid} HTTP/1.1\r\nUser-Agent: UDAP/2.0 \r\nHOST: $host\r\n\r\n""", hubitat.device.Protocol.LAN, host,  [callback: deviceDescriptionHandler]))
  	}
}

private discoverDevices() {
	sendHubCommand(new hubitat.device.HubAction("lan discovery ${tvSelector}", hubitat.device.Protocol.LAN))
}

def getDevices() {
	state.devices = state.devices ?: [:]
}

def getVerifiedDevices() {
	getDevices().findAll{ it?.value?.verified == true }
}

void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.${tvSelector}", ssdpHandler)
}

void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction("lan discovery ${tvSelector}", hubitat.device.Protocol.LAN))
}

void deviceDescriptionHandler(hubitat.device.HubResponse hubResponse) {
	log.debug "Response: ${hubResponse.body}"
	def body = hubResponse.xml
    def devices = getDevices()
    def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
    if (device) {
    	device.value << [name:body?.device?.friendlyName?.text() + " (" + convertHexToIP(hubResponse.ip) + ")", ip:convertHexToIP(hubResponse.ip), port:body?.port?.text(), model:body?.device?.modelName?.text(), verified: true]
    } else {
    	log.error "Host returted description.xml that didn't exist"
    }
}

// Display pairing key on TV
public requestPairingKey() {
	log.debug "Display pairing key"
    def devices = getDevices()
    def host
    devices.each {
        if(it.value.mac == selectedTvs) host = "${it.value.ip}:${it.value.port}"
    }
    
    def headers = [:]
    headers.put("HOST", "${host}")
    headers.put("Content-Type", "application/atom+xml")
    
    def reqKey = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthKeyReq</type></auth>"
    
    def httpRequest = [
    	method:		"POST",
        path: 		"/roap/api/auth",
        body:		"$reqKey",
        headers:	headers
  	]
    log.debug "HTTP REQUEST: ${httpRequest}"    
    sendHubCommand(new hubitat.device.HubAction(httpRequest))
}

def ssdpHandler(evt) {
	log.debug "Device ssdpHandler Event: $evt.description"
    def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub] 
    
    def devices = getDevices()
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
    if (parsedEvent?.ssdpTerm?.contains(tvSelector)) {
    	if (devices."${ssdpUSN}") {
        	log.debug "Smart TV already in state..."
            def d = devices."${ssdpUSN}"
            boolean deviceChangedValues = false
            def ip = convertHexToIP(parsedEvent.networkAddress)     
            def port = convertHexToInt(parsedEvent.deviceAddress)
            if(d.ip != ip || d.port != port) {
                log.debug "Device's port or ip changed..."
                log.debug "Current Address: ${ip}:${port} Old Address: ${d.ip}:${d.port}"
            	d.ip = ip
                d.port = port
                def child = getChildDevice(parsedEvent.mac)
                if(child) {
                    child.sync(ip, port)
                }
            }
        } else {
        	log.debug "Adding Smart TV to state..."
        	devices << ["${ssdpUSN}": parsedEvent]
        }
    } else {
    	log.debug "Smart TV not found..."
    }
}

// Helper functions
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
