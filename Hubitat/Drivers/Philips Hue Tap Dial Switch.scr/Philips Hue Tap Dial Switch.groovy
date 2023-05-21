/**
 *  Philips Hue Tap Dial Switch
 *
 *  Copyright 2023 John Goughenour
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
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.device.Protocol

public static String version()      {  return "v1.0.0"  }
public static String name()         {  return "Philips Hue Tap Dial Switch"  }
public static String driverInfo()   {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }
public static  Integer defaultDelay()   {  return 333  }    //default delay to use for zigbee commands (in milliseconds)
public static Double minVolt()          {  return 2.3  }    //default minimum volts to use for battery volt commands
public static Double maxVolt()          {  return 3.0  }    //default default maximum volts to use for battery volt commands

metadata {
	definition (name: name(), namespace: "tosh", author: "John Goughenour") {
        capability "Configuration"
        capability "PushableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        capability "Battery"
        capability "Refresh"
        
        attribute "batteryLastReplaced", "String"
        attribute "healthStatus", "enum", ["offline", "online"]
        
        command "resetBatteryReplacedDate"
        
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,FC00,1000", outClusters:"0019,0000,0003,0004,0006,0008,0005,1000", model:"RDM002", manufacturer:"Signify Netherlands B.V."
	}
    
    preferences {
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input(name: "batteryInterval", type: "number", title: "<b>Battery Reporting Interval</b>", defaultValue: 12, 
              description: "Set battery reporting interval by this many <b>hours</b>.", required: false)
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A Zigbee driver for Philips Hue Tap Dail Switch. ${driverInfo()}"
  }
}

def installed(){
	if(infoLogging) log.info "${device.displayName} is installing"
	initialize()
}

def updated(){
	if(infoLogging) log.info "${device.displayName} is updating"
	unschedule()
    configure()
	initialize()
}

def initialize(){
    if(infoLogging) log.info "${device.displayName} is  initializing"
    refresh()
}

def refresh() {
	if(infoLogging) log.info "${device.displayName} is refreshing"
    def cmd = [
        "he rattr 0x${device.deviceNetworkId} 1 ${zigbee.POWER_CONFIGURATION_CLUSTER} 0x0021 {}","delay ${defaultDelay}",  //battery
        "he rattr 0x${device.deviceNetworkId} 1 0x0005 0x0000 {}","delay ${defaultDelay}",  //battery
    ]
    sendZigbeeCommands cmd
}

def configure() {
	if(infoLogging) log.info "${device.displayName} is configuring setup"
    int reportInterval = batteryInterval.toInteger() * 60 * 60
    sendZigbeeCommands(["zdo bind ${device.deviceNetworkId} 0x01 0x0006 {${device.zigbeeId}} {}", "delay ${defaultDelay}", ]) // 
    sendZigbeeCommands(["zdo bind ${device.deviceNetworkId} 0x01 0x0008 {${device.zigbeeId}} {}", "delay ${defaultDelay}", ]) // 
    
}

def resetBatteryReplacedDate() {
    sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
}

void parse(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)    
    log.debug "Raw Message: ${description}"
    log.debug "Parsed Message: ${descMap}"
    
    def event = zigbee.getEvent(description)
    log.debug "Event Message: ${event}"
    log.debug "Button Number: ${zigbee.convertHexToInt(descMap?.data[2])}"
}

void sendZigbeeCommands(ArrayList<String> cmd) {
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmd.each {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
    }
    sendHubCommand(allActions)
}
