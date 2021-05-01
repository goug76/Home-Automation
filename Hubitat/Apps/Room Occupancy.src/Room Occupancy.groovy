/**
 *  Room Occupancy
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
    name: "Room Occupancy",
    namespace: "goug76",
    author: "John Goughenour",
    description: "Child app for Home Automation SmartApp.  Used to setup room automation based on if someone is in the room.",
    category: "My Apps",
    parent: "goug76:Smart Home Automation",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png")


preferences {	
	page(name: "mainPage")
    page(name: "triggers")
    page(name: "devices")
    page(name: "delays")
    page(name: "restrictions")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Room Occupancy Setup", install: true, uninstall: true) {
    	section(hideable: true, hidden: true, "Disable Room") {
            input("disable", "bool", title: "Disable Smartapp", defaultValue: false, submitOnChange: true)
            input("virtual", "bool", title: "Create Switch to enable/disable app")
        }
        if(!disable) {
         	section("Triggers") {href "triggers", title: "Choose Triggers", description: ""} 
            section("Devices") {href "devices", title: "Select Devices & Modes", description: ""}
            section("Delays") {href "delays", title: "Select Delays", description: ""}
            section("Restrictions") {href "restrictions", title: "Select Restrictions", description: ""}
    	}
        
        section("Additional Options") {
        	label title: "Name Room", required: false
            mode title: "Only run during specific mode(s)", required: false
        	input description: "During normal opperation the Room Occupancy app will only report status changes (On/Off) in the event log. For more detailed debug logging turn on 'Detailed Logging'", 
            	type: "paragraph", element: "paragraph", title: "Logging"
            input(name: "detailedLog", type: "bool", title: "Detailed Logging",defaultValue: true, required: false)
        }
	}
}

def triggers() {
	dynamicPage(name: "triggers", title: "Select Triggers", nextPage: "mainPage") {
    	section("When these sensors are triggered") {
    		input "motion", "capability.motionSensor", title: "Which Motion Sensor?", hideWhenEmpty: true, multiple: true, required: false
            input "contact", "capability.contactSensor", title: "Which Contact Sensor?", hideWhenEmpty: true, multiple: true, required: false
            input "presence ", "capability.presenceSensor", title: "Which Presence  Sensor?", hideWhenEmpty: true, multiple: true, required: false
        }
    }
}

def devices() {
	dynamicPage(name: "devices", title: "Select Devices & Modes", nextPage: "mainPage") {
    	section("Turn on these devices") {
        	input(name: "switches", type: "capability.switch", title: "Which Devices?", multiple: true, submitOnChange: true)
            if (switches.find{it.hasCommand('setLevel')}) {
            	input "level", "enum", title: "Set Dimmer Level", options: [[10:"10%"],[15:"15%"],[20:"20%"],[25:"25%"],[30:"30%"],[35:"35%"],[40:"40%"],[45:"45%"],
                                                                            [50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "100"
            }
        }
        section("Evening/Morning Modes") {
        	input("evening", "bool", title: "Set Evening/Morning Mode", description: "Allows you to select differnt settings based on your evening/morning modes.", submitOnChange: true)
            if(evening) {
            	input "eveningMode", "mode", title: "Select Evening/Morning modes", multiple: true, required: false
                input("diffEvening", "bool", title: "Set Evening/Morning Switches", description: "Allows you to select differnt switches based on your evening/morning modes.", submitOnChange: true)
                if(diffEvening) {
                	input(name: "eveningSwitches", type: "capability.switch", title: "Which Devices?", multiple: true, submitOnChange: true)
                }
                if ((diffEvening && eveningSwitches.find{it.hasCommand('setLevel')}) || switches.find{it.hasCommand('setLevel')}) {
                    input "eveningLevel", "enum", title: "Set Dimmer Level", options: [[10:"10%"],[15:"15%"],[20:"20%"],[25:"25%"],[30:"30%"],[35:"35%"],[40:"40%"],[45:"45%"],
                                                                                       [50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "50"
                }
            }
        }
        section("Night/Bedtime Modes") {
        	input("nite", "bool", title: "Set Night/Bedtime Modes", description: "Allows you to select differnt settings based on your night/bedtime modes.", submitOnChange: true)
            if(nite) {
            	input "niteMode", "mode", title: "Select Night/Bedtime modes", multiple: true, required: false
                input("difNite", "bool", title: "Set Night/Bedtime Switches", description: "Allows you to select differnt switches based on your night/bedtime modes.", submitOnChange: true)
                if(difNite) {
                	input(name: "niteSwitches", type: "capability.switch", title: "Which Devices?", multiple: true, submitOnChange: true)
                }
                if ((difNite && niteSwitches.find{it.hasCommand('setLevel')}) || switches.find{it.hasCommand('setLevel')}) {
                    input "niteLevel", "enum", title: "Set Dimmer Level", options: [[10:"10%"],[15:"15%"],[20:"20%"],[25:"25%"],[30:"30%"],[35:"35%"],[40:"40%"],[45:"45%"],
                                                                                    [50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "10"
                }
            }
        }
    }
}

def delays() {
	dynamicPage(name: "delays", title: "Select Delays", nextPage: "mainPage") {
    	section("Turn devices off after this many minutes of no activity") {
        	input "offMinutes", "number", title: "Minutes", required: false
        }
        if(evening) {
        	section("Turn devices off after this many minutes of no activity for evening/morning modes") {
            	input "eveningMinutes", "number", title: "Minutes", required: false
            }
        }
        if(nite) {
        	section("Turn devices off after this many minutes of no activity for night/bedtime modes") {
            	input "niteMinutes", "number", title: "Minutes", required: false
            }
        }
    }
}

def restrictions() {
	dynamicPage(name: "restrictions", title: "Select Restrictions Only if True", nextPage: "mainPage") {
    	section("Only Run Between these Times") {
        	input("betweenTime", "bool", title: "Set times", submitOnChange: true)
            if(betweenTime) {
            	input "fromTime", "time", title: "From", required: True
              	input "toTime", "time", title: "To", required: True
            }
        }
        section("Only Run Between Sunset and Sunrise") {
        	input("setRise", "bool", title: "Select Between Sunset and Sunrise", submitOnChange: true)
        }
        section("Illuminance Sensor", hideWhenEmpty: true) {
        	input(name: "luxSensor", type: "capability.illuminanceMeasurement", title: "Which Lux Sensor?", multiple: true, submitOnChange: true)
            if(luxSensor) {
            	input "lux", "number", title: "Lux Level", required: false
            }
        }
        section("If at least one light is on don't turn on others") {
        	input("otherLights", "bool", title: "Disable Other Lights", submitOnChange: true)
        }
    }
}

def installed() {
	if (detailedLog) log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	if (detailedLog) log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule(checkMotion)
	initialize()
}

def uninstalled() {
	if (detailedLog) log.debug "Uninstalling Room Occupancy: ${app.label}"
	if(atomicState.deviceID) removeChildDevices()
}

def initialize() {
	if(virtual) { 
    	atomicState.deviceID = "${app.id}_Disable_${app.label}"
        if (!getChildDevice(atomicState.deviceID)) createVirtualSwitch(atomicState.deviceID)        
    } else { 
    	if(atomicState.deviceID) removeChildDevices()
    }   
    if(disable) {
        if(getChildDevice(atomicState.deviceID)) getChildDevice(atomicState.deviceID).on()
    } else {
        if(getChildDevice(atomicState.deviceID)) getChildDevice(atomicState.deviceID).off()
    }    
    if(setRise) setAstro()
    subscribeEvents(getMode())
}

def subscribeEvents(mode) {
    def curSwitches = getSwitches(mode)
    subscribe(switches, "switch.on", switchOnHandler)
    //subscribe(curSwitches, "switch.on", switchOnHandler)
    if(getChildDevice(atomicState.deviceID)) subscribe(getChildDevice(atomicState.deviceID), "switch", disableHandler)
    if(setRise) subscribe(location, "sunset", astroHandler)
    if(setRise) subscribe(location, "sunrise", astroHandler)
	if(motion) subscribe(motion, "motion.active", triggerHandler)
    if(motion) subscribe(motion, "motion.inactive", motionStoppedHandler)
    if(contact) subscribe(contact, "contact.open", triggerHandler)
    if(presence) subscribe(presence, "presence.present", triggerHandler)
    subscribe(location, "mode", modeHandler)
}

def triggerHandler(evt) {
	if (detailedLog) log.debug "Room Occupancy ${app.label} => ${evt.name} triggered"
    unschedule(checkMotion)
	if(!disable) {    	
        if(checkRestrictions()) {
            def mode = getMode()
            def curSwitches = getSwitches(mode)
            def curLevel = getLevel(mode)
            def delay = getDelay(mode)
        	if(otherLights) {
            	if (!curSwitches.count {it.currentValue('switch') == 'on'}) {
                	curSwitches.each {
                    	if (detailedLog) log.debug("Dimming: $it.displayName setLevel($curLevel)")
                        it.setLevel(curLevel as Integer)
                    }
                }
            } else {
                curSwitches.each {
                    if (it.hasCommand('setLevel')) {
                        if (detailedLog) log.debug("Dimming: $it.displayName setLevel($curLevel)")
                        it.setLevel(curLevel)
                    }
                    it.on()
                }
            }
        }
        if(evt.name != "motion" && delay) runIn(60 * delay, checkMotion)
    }
}

def switchOnHandler(evt) {
    unschedule(checkMotion)
    if(!disable) {
        def mode = getMode()
        def curLevel = getLevel(mode)
        def delay = getDelay(mode)
        if (detailedLog) log.debug "switchOnHandler called -> setting timer for $delay minutes"
        if (evt.device.hasCommand('setLevel')) {
            if (detailedLog) log.debug("Dimming: $evt.displayName setLevel($curLevel)")
            evt.device.setLevel(curLevel as Integer)
        }
        if(delay) runIn(60 * delay, checkMotion)
    }
}

def astroHandler(evt) {
    if (detailedLog) log.debug "Room Occupancy ${app.label} => ${evt.name} triggered"
    switch(evt.name) {
        case "sunset":
            state.astro = "set"
            break
        case "sunrise": 
            state.astro = "rise"
        break
    }
}

def motionStoppedHandler(evt) {
    unschedule(checkMotion)
    if(!disable) {
    	def mode = getMode()
        def delay = getDelay(mode)
        if (detailedLog && delay) log.debug "motionStoppedHandler called -> setting timer for $delay minutes"
        if(delay) runIn(60 * delay, checkMotion, [overwrite: false])
    }
}

def modeHandler(evt) {
    if (detailedLog) log.debug "Room Occupancy ${app.label} => ${evt.name} triggered"
    //unsubscribe()
    def mode = getMode()
    //subscribeEvents(mode)
    def curSwitches = getSwitches(mode)
    if (curSwitches.count {it.currentValue('switch') == 'on'}) {
        def curLevel = getLevel(mode)
        curSwitches.each {
            if (it.hasCommand('setLevel') && it.currentValue('switch') == 'on') {
                if (detailedLog) log.debug("Dimming: $it.displayName setLevel($curLevel)")
                it.setLevel(curLevel as Integer)
            }
        }
    }
}

def disableHandler(evt) {
	log.debug "$evt.displayName Switch was activated, disabling ${app.label}"
	unschedule(checkMotion)
    switch (evt.value) {
        case 'on':
            app.updateSetting("disable", [type: "bool", value: true])
            break
        case 'off':
            app.updateSetting("disable", [type: "bool", value: false])
            break
    }
}

def checkMotion() {
    if(!disable) {
        if (detailedLog) log.debug "In checkMotion scheduled method"
    	def mode = getMode()
        def noMotion = true
        def motionCount = 0
        motion.each {
            def motionState = it.currentState("motion")
            if (motionState.value == "active") {
                if (detailedLog) log.debug "Motion is active on $it.label"
                motionCount++
            }
        }
        if(motionCount > 0) noMotion = false
        if (noMotion) {
        	def curSwitches = getSwitches(mode)
            log.debug "No motion detected turning $curSwitches off"
            curSwitches.off()
        }
    }
}

private String getMode() {
	def mode
    if(evening && eveningMode.find{ it == location.currentMode.name}) mode = 'eveningMode'
    if(nite && niteMode.find{ it == location.currentMode.name}) mode = 'niteMode'
    if(!mode) mode = 'normal'
    return mode
}

private setAstro() {
    def dateNow = now()
    def sun = getSunriseAndSunset()
    if(dateNow > sun.sunset.time || dateNow < sun.sunrise.time) state.astro = "set" else state.astro = "rise"
}

private getSwitches(mode) {
	def curSwitches
	switch (mode) {
    	case 'normal':
        	curSwitches = switches
            break
        case 'eveningMode':
        	if(diffEvening) curSwitches = eveningSwitches else curSwitches = switches
            break
        case 'niteMode':
        	if(difNite) curSwitches = niteSwitches else curSwitches = switches
            break
    }
    return curSwitches
}

private getLevel(mode) {
	def curLevel
	switch (mode) {
    	case 'normal':
        	curLevel = level
            break
        case 'eveningMode':
        	if(evening && eveningLevel) curLevel = eveningLevel else curLevel = level
            break
        case 'niteMode':
        	if(nite && niteLevel) curLevel = niteLevel else curLevel = level
            break
    }
    return curLevel
}

private getDelay(mode) {
	def curDelay
	switch (mode) {
    	case 'normal':
        	curDelay = offMinutes
            break
        case 'eveningMode':
        	curDelay = eveningMinutes
            break
        case 'niteMode':
        	curDelay = niteMinutes
            break
    }
    return curDelay
}

private checkRestrictions() {
	def noRestrictions = true
    if(noRestrictions && betweenTime) {
        if(!timeOfDayIsBetween(toDateTime(fromTime), toDateTime(toTime), new Date(), location.timeZone)) {
        	if (detailedLog) log.debug "Not between $fromTime and $toTime, action will not fire"
            noRestrictions = false
        }
    }
    if(noRestrictions && setRise) {
    	if(state.astro == "rise") {
            if (detailedLog) log.debug "Not between Sunset and Sunrise, action will not fire => Astro State: ${state.astro}"
            noRestrictions = false
        }
    }
    if(noRestrictions && luxSensor) {
    	def curLux = luxSensor.currentIlluminance
        curLux.each {
            if(it > lux) {
        	    if (detailedLog) log.debug "Current lux level $it is greater than the lux setpoint $lux, action will not fire"
                noRestrictions = false
            }
        }
    }
    return noRestrictions
}

private createVirtualSwitch(id) {
	if (detailedLog) log.debug "Creating virtual switch with a device ID => $id"
	def vName = "Disable ${app.label}"
    addChildDevice("hubitat", "Virtual Switch", id, null, ["name": vName, "label": vName, completedSetup: true])
    getChildDevice(atomicState.deviceID).on()
}

private removeChildDevices() {
	if (detailedLog) log.debug "Deleting virtual swith with device ID => $atomicState.deviceID"
	deleteChildDevice(atomicState.deviceID)
    atomicState.deviceID = ""
}
