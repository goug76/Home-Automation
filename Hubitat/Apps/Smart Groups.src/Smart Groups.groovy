/**
 *  Smart Groups
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
 
 public static String version()      {  return "v1.0.0"  }
 
definition(
    name: "Smart Groups",
    namespace: "goug76",
    author: "John Goughenour",
    description: "This app will allow you to group devices together and spawn a child device to control the group.",
    category: "My Apps",
    parent: "goug76:Smart Home Automation",
    iconUrl: "http://cdn.device-icons.smartthings.com/secondary/smartapps-tile@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/secondary/smartapps-tile@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/secondary/smartapps-tile@2x.png")


preferences {
	page(name: "MainPage")
}

def MainPage() {
	dynamicPage(name: "MainPage", title: "", install: true, uninstall: true) {
    	section("<b>Control these devices and lights with group</b>") {
        	input(name: "switches", type: "capability.switch", title: "Which Devices?", multiple: true, submitOnChange: true)
            if (switches.find{it.hasCommand('setLevel')} != null) {
            	input(name: "dimOff", type: "bool", title: "Turn OFF Devices", required: false,
                	description: "This group contains dimmable switches. When setting the dim level for the group non-dimmable switches will remain on. If you want the non-dimmable switches to turn off enable 'Turn OFF Devices'")
                paragraph "This group contains dimmable switches. When setting the dim level for the group non-dimmable switches will remain on. If you want the non-dimmable switches to turn off enable 'Turn OFF Devices'"
            }
            input "deviceName", "text", title: "Child Device Group Name", required: true, description: "Name virtual Switch to be created to control group"
        }
        section("<b>App Settings</b>") {
        	label title: "Group Name", required: true
            mode title: "Only run during specific mode(s):", required: false
            input(name: "detailedLog", type: "bool", title: "Detailed Logging", defaultValue: "true", description: "During normal opperation the ${app.name} will only report status changes (On/Off) in the event log. For more detailed debug logging turn on 'Detailed Logging'", required: false)
        }
        section("<p style=\"border-bottom:1px solid\"><b>About</b></p>") {
            paragraph "<p style=\"text-align:center\"><strong>John Goughenour</strong> (Goug76)</br>${app.name}</br><em>${version()}</em></p>"
        }
    }
}

def installed() {
	if (detailedLog) log.debug "Installing Smart Group: ${app.label} => settings: ${settings}"
    atomicState.deviceID = "${app.getId()}"
	createGroup()
	initialize()
}

def updated() {
	if (detailedLog) log.debug "Updated Smart Group: ${app.label} => with settings: ${settings}"

	unsubscribe()
	initialize()
}

def uninstalled() {
	if (detailedLog) log.debug "Uninstalling Smart Group: ${app.label}"
	deleteGroup()
}

def initialize() {
	if (detailedLog) log.debug "${app.label} initializing"
    atomicState.noSync = false
    checkDriver()
    subscribe(switches, "switch", eventHandler)
    subscribe(switches, "level", eventHandler)
    def attribs
    def group = getChildDevice(atomicState.deviceID)
    switch(group.typeName) {
    	case "Smart Group Dimming":
            attribs = ["switch", "level"]
            break
     	case "Smart Group Switch":
        	attribs = ["switch"]
            break
    }
    group.label = deviceName
    syncGroup(attribs)
}

def createGroup () {
    if (detailedLog) log.debug "Creating Smart Group Device: ${deviceName} => Device ID: ${atomicState.deviceID}"
    def hasLevel = switches.find { it.hasCommand('setLevel') }
    if (hasLevel) {
    	log.debug "has level"
    	addChildDevice("goug76", "Smart Group Dimming", atomicState.deviceID, null, ["name": deviceName, "label": deviceName, completedSetup: true])
    } else {
    	log.debug "is Switch"
    	addChildDevice("goug76", "Smart Group Switch", atomicState.deviceID, null, ["name": deviceName, "label": deviceName, completedSetup: true])
    }
}

def deleteGroup() {
	if (detailedLog) log.debug "Deleting Smart Group Device: ${getChildDevice(atomicState.deviceID).label} => Device ID: ${atomicState.deviceID}"
	deleteChildDevice(atomicState.deviceID)
}

void checkDriver() {
	if (detailedLog) log.debug "Checking Device Driver"
	def driver = getChildDevice(atomicState.deviceID)
    def group = getChildDevice(atomicState.deviceID)
    def hasLevel = switches.find { it.hasCommand('setLevel') }
    if (hasLevel) {
    	log.debug "Driver: ${driver.typeName} has level"
    	if (driver.typeName.contains('Dimming')) group.updateDriver("Match") else group.updateDriver("Mismatch")
    } else {
    	log.debug "Driver: ${driver.typeName} is Switch"
    	if (driver.typeName.contains('Switch')) group.updateDriver("Match") else group.updateDriver("Mismatch")
    }
}

def eventHandler(evt) {
	if (detailedLog) log.debug "Device state change: ${evt.device.displayName} => ${evt.name} = ${evt.value}"
    if(!atomicState.noSync) syncGroup(evt?.collect {it.name})
}

def syncGroup(names) {
    def group = getChildDevice(atomicState.deviceID)
    names?.each { name ->
        def values = switches."current${name.capitalize()}"
        values?.removeAll([null])
        if (detailedLog) log.debug "Updating Group State: $name => $values"
  		group.sync(name, values)
    }
}

def groupCommand(cmnd, arg = null) {
    atomicState.noSync = true
    if (detailedLog) log.debug "noSync: ${atomicState.noSync}"
	if(arg) {
		if (detailedLog) log.debug "${app.label} Received groupCommand from ${getChildDevice(atomicState.deviceID).label} => command: $cmnd, Level: $arg"
    	switches.each {
        	if (it.hasCommand('setLevel') && arg > 0) {
            	it."$cmnd"(arg)
                it.on()
            }  else if (dimOff && arg < 100) {
            	it.off()
            } else {
            	if (arg == 0) { it.off() } else { it.on() }
            }
        }
    } else {
		if (detailedLog) log.debug "${app.label} Received groupCommand from ${getChildDevice(atomicState.deviceID).label} => command: $cmnd"
    	switches?."$cmnd"()
    }
    runIn(5, "allowSync")
}

private allowSync() {
    atomicState.noSync = false
    if (detailedLog) log.debug "noSync: ${atomicState.noSync}"
}
