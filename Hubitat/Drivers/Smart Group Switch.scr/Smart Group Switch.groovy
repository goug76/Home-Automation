/**
 *  Smart Group Switch
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
 public static String version()      {  return "v1.0.0"  }
 public static String name()         {  return "Smart Group Switch"  }
metadata {
	definition (name: name(), namespace: "goug76", author: "John Goughenour", cstHandler: true) {
		capability "Switch"
        
        attribute "percentageOn", "number"
        attribute "driver", "string"
	}
    
    preferences {
    	input(name: "detailedLog", type: "bool", title: "<b>Detailed Logging</b>", defaultValue: "true", description: "During normal opperation the ${name()} will only report status changes (On/Off) in the event log. For more detailed debug logging turn on 'Detailed Logging'", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", description: "<p style=\"text-align:center\"><strong>John Goughenour</strong> (goug76)</br>${name()}<br/><em>${version()}</em></p>"
    }
}

def installed(){
	if (detailedLog) log.debug "$device.displayName installed"    
    sendEvent(name: "driver", value: "Match")
	initialize()
}

def updated(){
	unschedule()
	initialize()
}

def initialize(){
	if (detailedLog) log.debug "$device.displayName initializing"
}

// handle commands
def on() {
	if (detailedLog) {log.debug "$device.displayName on command received, status: switch - ${device.currentValue("switch")}"}
	sendEvent(name: "percentage", value: 100)
	sendCommand("on", true)
}

def off() {
	if (detailedLog) {log.debug "$device.displayName off command received, status: switch - ${device.currentValue("switch")}"}
	sendEvent(name: "percentage", value: 0)
	sendCommand("off", true)
}

private sendCommand(cmnd, trigger) {
	if (detailedLog) log.debug "$device.displayName sendCommand command received: $cmnd"
	sendEvent(name: "switch", value: cmnd)
    if(trigger) { parent.groupCommand(cmnd) }
}

def sync(name, values) {
	if (detailedLog) log.debug "$parent.label sent syncGroup command to $device.displayName"
	"${name}Sync"(values)
}

def updateDriver(status) {
	if (detailedLog) log.debug "Update Device Driver Status to $status"
	sendEvent(name: "driver", value: "${status}")
}

private switchSync(values) {
	if (detailedLog) log.debug "$device.displayName syncSwitch command received values: $values"
    def onCount = values?.count {it.toUpperCase() == "ON"}
    def cent = (int)Math.floor((onCount / values?.size()) * 100)
    if (detailedLog) log.debug "On Count: $onCount ~ Percent On: $cent"
    if (cent == 0) {
    	if (detailedLog) log.debug "Percent is: $cent, setting ${device.displayName} to off"
        sendCommand("off", false)
        sendEvent(name: "percentageOn", value: cent)
    } else {
    	if (detailedLog) log.debug "Percent is: $cent, setting ${device.displayName} to on"
        sendCommand("on", false)
        sendEvent(name: "percentageOn", value: cent)
    }
}
