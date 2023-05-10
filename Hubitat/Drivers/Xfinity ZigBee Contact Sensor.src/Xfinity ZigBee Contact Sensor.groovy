/**
 *  Xfinity ZigBee Contact Sensor
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
public static String name()         {  return "Xfinity ZigBee Contact Sensor"  }
def Integer defaultDelay = 333       //default delay to use for zigbee commands (in milliseconds)

metadata {
	definition (name: name(), namespace: "goug76", author: "John Goughenour") {
        capability "Configuration"
        capability "Sensor"
        capability "ContactSensor"
        capability "Battery"
        capability "TemperatureMeasurement"
        capability "TamperAlert"
        capability "Refresh"
        
        attribute "batteryLastReplaced", "String"
        
        command "resetBatteryReplacedDate"
        
        fingerprint deviceJoinName: name(), profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05,FD50", outClusters:"0019", model:"LDHD2AZW", manufacturer:"Leedarson"
        fingerprint deviceJoinName: name(), profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"URC4460BC0-X-R", manufacturer:"Universal Electronics Inc"
        fingerprint deviceJoinName: name(), profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"MCT-350 SMA", manufacturer:"Visonic"
	}
    
    preferences {
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Test</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input(name: "batteryInterval", type: "number", title: "<b>Battery Reporting Interval</b>", defaultValue: 12, 
              description: "Set battery reporting interval by this many <b>hours</b>.", required: false)
        input(name: "tempInterval", type: "number", title: "<b>Temperature Reporting Interval</b>", defaultValue: 0, 
              description: "Set temperature reporting interval by this many <b>minutes</b>. </br>Default: 0 (Disabled)", required: false)
        input name: "tempOffset", title: "<b>Temperature Calibration</b>", type: "number", range: "-128..127", defaultValue: 0, required: true, 
            description: "Adjust temperature by this many degrees.</br>Range: -128 thru 127</br>Default: 0"
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A Zigbee driver for Xfinity door and window sensors. <p style=\"text-align:center\"></br><strong>John Goughenour</strong> (goug76)</br>${name()}<br/><em>${version()}</em></p>"
  }
}

def installed(){
	initialize()
}

def updated(){
	unschedule()
    configure()
	initialize()
}

def initialize(){
    refresh()
}

def refresh() {
    def cmd = [
        "he rattr 0x${device.deviceNetworkId} 1 0x0402 0x0000 {}","delay 200",  //temp
        "he rattr 0x${device.deviceNetworkId} 1 ${zigbee.POWER_CONFIGURATION_CLUSTER} 0x0020 {}","delay 200",  //battery
    ]
    return cmd
}

def configure() {
    int reportInterval = batteryInterval.toInteger() * 60 * 60
    List cmd = ["zdo bind 0x${device.deviceNetworkId} 1 1 0x0500 {${device.zigbeeId}} {}", "delay ${defaultDelay}",] // IAS Zone]
    cmd += zigbee.enrollResponse(1200)
    cmd += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020, DataType.UINT8, 0, reportInterval, 1, [:], 200)   // Configure Voltage - Report once per 12hrs or if a change of 100mV detected
    cmd += zigbee.temperatureConfig(0,(tempInterval.toInteger() * 60))
    cmd += refresh()
    
    return cmd
}

def resetBatteryReplacedDate() {
    sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
}

void parse(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)    
    def result = []

    if (description?.startsWith("enroll request")) {
        List cmds = zigbee.enrollResponse(1200)        
        if(debugLogging) log.debug "enroll response: ${cmds}"        
        result = cmds?.collect { new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE) }
    } else if (description?.startsWith('zone status') || description?.startsWith('zone report')) {
        if(debugLogging) log.debug "Zone status: ${description}"
        parseIasMessage(description)
    } else {
        if(debugLogging) log.debug "Attribute Message: ${descMap}"
        parseAttributeMessage(description)
    }
}

private parseIasMessage(String description) {
    ZoneStatus zs = zigbee.parseZoneStatus(description)
    zs.tamper ? sendEvent(name:'tamper', value: 'detected', descriptionText: "${device.displayName} tamper detected.", type: 'physical') : sendEvent(name:'tamper', value: 'clear', descriptionText: "${device.displayName} tamper is cleared.", type: 'physical')
    zs.alarm1 ? sendEvent(name:'contact', value: 'open', descriptionText: "${device.displayName} is open.", type: 'physical') : sendEvent(name:'contact', value: 'closed', descriptionText: "${device.displayName} is closed.", type: 'physical')
}

private parseAttributeMessage(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)
    switch(descMap.cluster) {
        case '0402':
            handleTemperatureEvents(descMap)
            break
        case '0001':
            handleBatteryEvents(descMap)            
            break
    }
}

private handleBatteryEvents(descMap) {
    def result = [:]
    if (descMap.value) {
        def rawValue = zigbee.convertHexToInt(descMap.value)
        def minVolts = 2.3
		def maxVolts = 3.0
		def volts = rawValue / 10
        def pct = (volts - minVolts) / (maxVolts - minVolts)
        def roundedPct = Math.round(pct * 100)
        if (roundedPct <= 0) roundedPct = 1
        result.value = Math.min(100, roundedPct)
        result.descriptionText = "${device.displayName} battery is ${result.value}%"
        result.name = 'battery'
        result.isStateChange = true
        result.type = 'physical'
        result.unit = "%"
        if(infoLogging) log.info "${result.descriptionText}"
        sendEvent(result)
    }
    return result
}

private handleTemperatureEvents(descMap) {
    def result = [:]
    if (descMap.value) {
        def rawValue = hexStrToSignedInt(descMap.value) / 100
        result.value = convertTemperatureIfNeeded(rawValue.toFloat(),"c",2)
        if(tempOffset) result.value = (result.value.toFloat() + tempOffset.toFloat()).round(2)
        result.name = 'temperature'
        result.isStateChange = true
        result.type = 'physical'
        result.unit = "°${location.temperatureScale}"
        result.descriptionText = "${device.displayName} temperature is ${result.value}${result.unit}"
        if(infoLogging) log.info "${result.descriptionText}"
        sendEvent(result)
    }
    return result
}
