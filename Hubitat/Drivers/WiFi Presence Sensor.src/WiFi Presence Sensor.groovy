/**
 *  Wi-Fi Presence Sensor
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
 *
 **********************************************************************************************************************/
import groovy.json.*

public static String version()          {  return "v1.0.0"  }
public static String name()             {  return "Wi-Fi Presence Sensor"  }
public static String driverInfo()       {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }
	
metadata {
	definition (name: name(), namespace: "tosh", author: "John Goughenour") {
		capability "Refresh"
		capability "Sensor"
        capability "PresenceSensor"
	}

	preferences {
        input (type: "string", name: "ipAddress", title: "<b>Phone IP Address</b>", required: true)
		input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "false", description: "", required: false)
        input (type: "number", name: "timeout", title: "<b>Timeout</b>", range: "1..99", required: true, defaultValue: 3, 
		    description: "Approximate number of minutes without a response before device is not present.</br>Range: 1-99")
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A simple Wi-Fi presence sensor for detecting family members based on their phones connection to your home network. ${driverInfo()}"
	}
}

def installed () {
	if(infoLogging) log.info "${device.displayName} is installed()"
    updated()
}

def updated () {
	if(infoLogging) log.info "${device.displayName} is updated()"
    
    state.tryCount = 0    
    runEvery1Minute(refresh)
    runIn(2, refresh)
}


def refresh() {
	if(infoLogging) log.info "${device.displayName} is refresh()"
    if (ipAddress == null || ipAddress.size() == 0) {
        if(debugLogging) log.debug "${device.displayName} has no IP address"
		return
	}

	state.tryCount++
    
    if (state.tryCount > timeout) {
        def descriptionText = "${device.displayName} is not present";
        if(infoLogging) log.info descriptionText
        sendEvent(name: "presence", value: "not present", type: 'virtual', descriptionText: descriptionText)
    }
    
	asynchttpGet("httpGetCallback", [uri: "http://${ipAddress}/"])
}


def httpGetCallback(response, data) {
	if(debugLogging) log.debug "${device.displayName}: httpGetCallback(${groovy.json.JsonOutput.toJson(response)}, data)"
	
	if (response && response.status == 408 && response.errorMessage.contains("Connection refused")) {
		state.tryCount = 0
		
		def descriptionText = "${device.displayName} is present";
		if(infoLogging) log.info descriptionText
		sendEvent(name: "presence", value: "present", type: 'virtual', descriptionText: descriptionText)
	}
}
