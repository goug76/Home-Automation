/**
 *  Smart Home Automation
 *
 *  Copyright 2018 John Goughenour
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
    name: "Smart Home Automation",
    namespace: "goug76",
    author: "John Goughenour",
    description: "Parent app for all smart home automatons.",
    category: "Convenience",
    singleInstance: true,
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home5-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home5-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home5-icn@2x.png")


preferences {
	page(name: "mainPage")
    page(name: "roomOccupancy")
    page(name: "smartGroups")
    page(name: "abc")
    page(name: "garageDoor")
    page(name: "groups")
    page(name: "assist")
    page(name: "smartTV")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Your Automations", install: true, uninstall: true, submitOnChange: true) {
        section("Smart Groups") { href "smartGroups", title: "Your Smart Groups", description: "", image: "http://cdn.device-icons.smartthings.com/secondary/smartapps-tile@2x.png"}  
    	section("Room Occupancy") {href "roomOccupancy", title: "Your Rooms", description: "", image: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png"}        
        //section("Advanced Button Controller") {href "abc", title: "Your Buttons", description: "", image: "https://raw.githubusercontent.com/paulsheldon/SmartThings-PS/master/resources/abc/images/abcNew.png"}    
        section("Smart TVs") { href "smartTV", title: "Your Smart TVs", description: "", image: "http://cdn.device-icons.smartthings.com/Electronics/electronics15-icn@2x.png"}   
        section("Garage Doors") { href "garageDoor", title: "Your Garage Doors", description: "", image: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn@2x.png"}   
        //section("Groups") {href "groups", title: "Your Groups", description: "", image: "https://cdn.rawgit.com/Kriskit/SmartThingsPublic/master/smartapps/kriskit/trendsetter/icon@2x.png"}
        //section("Ecobee Mode Assist") {href "assist", title: "Your Modes", description: "", image: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"}
    }
}

def roomOccupancy() {
	return dynamicPage(name: "roomOccupancy", title: "Your Rooms", install: false, submitOnChange: true) {
    	section("") {
    		app(name: "rooms", appName: "Room Occupancy", namespace: "goug76", title: "Create Room...", multiple: true)
        }
    }
}

def smartGroups() {
	dynamicPage(name: "smartGroups", title: "Your Smart Groups", install: false, submitOnChange: true) {
    	section("") {
        	app(name: "smartGroups", appName: "Smart Groups", namespace: "goug76", title: "Create Smart Group...", multiple: true)
        }
    }
}

def smartTV() {
	dynamicPage(name: "smartTV", title: "Your Smart TVs", install: false, submitOnChange: true) {
    	section("") {
        	app(name: "smartTV", appName: "Smart TV (Connect)", namespace: "goug76", title: "Add Smart TV...", multiple: true)
        }
    }
}

def garageDoor() {
	dynamicPage(name: "garageDoor", title: "Your Garage Doors", install: false, submitOnChange: true) {
    	section("") {
        	app(name: "garageDoor", appName: "Virtual Garage Door", namespace: "peng1can", title: "Add Garage Door...", multiple: true)
        }
    }
}

def groups() {
	dynamicPage(name: "groups", title: "Your Groups", install: false, submitOnChange: true) {
    	section("") {
        	app(name: "groups", appName: "Group", namespace: "kriskit.trendSetter", title: "Create Group...", multiple: true)
        }
    }
}

def abc() {
	dynamicPage(name: "abc", title: "Advanced Button Controler", install: false, submitOnChange: true) {
    	section {
        	app(name: "abc", appName: "ABC Child Creator", namespace: "paulsheldon", title: "Create Button...", multiple: true)
        }
    }
}

def assist() {
	dynamicPage(name: "assist", title: "Your Modes", install: false, submitOnChange: true) {
    	section {
        	app(name: "ecobeeChangeMode", appName: "ecobeeChangeMode", namespace: "yracine", title: "Add Modes...", multiple: true)
        }
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers
