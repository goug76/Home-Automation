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

public static String version()          {  return "v1.0.0"  }
public static String name()             {  return "Xfinity ZigBee Contact Sensor"  }
public static String driverInfo()       {  return "<p style=\"text-align:center\"></br><strong><a href='https://thisoldsmarthome.com' target='_blank'>This Old Smart Home</a></strong> (tosh)</br>${name()}<br/><em>${version()}</em></p>"  }
public static Integer defaultDelay()    {  return 333  }    //default delay to use for zigbee commands (in milliseconds)
public static Double minVolt()          {  return 2.3  }    //default minimum volts to use for battery volt command
public static Double maxVolt()          {  return 3.0  }    //default maximum volts to use for battery volt command

metadata {
	definition (name: name(), namespace: "tosh", author: "John Goughenour") {
        capability "Configuration"
        capability "Sensor"
        capability "ContactSensor"
        capability "Battery"
        capability "TemperatureMeasurement"
        capability "TamperAlert"
        capability "Refresh"
        
        attribute "batteryLastReplaced", "String"
        
        command "resetBatteryReplacedDate"
        
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05,FD50", outClusters:"0019", model:"LDHD2AZW", manufacturer:"Leedarson"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"URC4460BC0-X-R", manufacturer:"Universal Electronics Inc"
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05", outClusters:"0019", model:"MCT-350 SMA", manufacturer:"Visonic"
	}
    
    preferences {
        input(name: "infoLogging", type: "bool", title: "<b>Enable Description Text</b>", defaultValue: "true", description: "", required: false)
        input(name: "debugLogging", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: "true", description: "", required: false)
        input(name: "batteryInterval", type: "number", title: "<b>Battery Reporting Interval</b>", defaultValue: 12, 
              description: "Set battery reporting interval by this many <b>hours</b>.", required: false)
        input(name: "tempInterval", type: "number", title: "<b>Temperature Reporting Interval</b>", defaultValue: 0, 
              description: "Set temperature reporting interval by this many <b>minutes</b>. </br>Default: 0 (Disabled)", required: false)
        input name: "tempOffset", title: "<b>Temperature Calibration</b>", type: "number", range: "-128..127", defaultValue: 0, required: true, 
            description: "Adjust temperature by this many degrees.</br>Range: -128 thru 127</br>Default: 0"
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A Zigbee driver for Xfinity door and window sensors. ${driverInfo()}"
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
        "he rattr 0x${device.deviceNetworkId} 1 0x0402 0x0000 {}","delay ${defaultDelay}",  //temp
        "he rattr 0x${device.deviceNetworkId} 1 ${zigbee.POWER_CONFIGURATION_CLUSTER} 0x0020 {}","delay ${defaultDelay()}",  //battery
    ]
    return cmd
}

def configure() {
    int reportInterval = batteryInterval.toInteger() * 60 * 60
    List cmd = ["zdo bind 0x${device.deviceNetworkId} 1 1 0x0500 {${device.zigbeeId}} {}", "delay ${defaultDelay()}",] // IAS Zone
    cmd += zigbee.enrollResponse(1200) // Enroll in IAS Zone
    cmd += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020, DataType.UINT8, 0, reportInterval, 1, [:], defaultDelay()) //Battery Voltage Reporting
    cmd += zigbee.temperatureConfig(0,(tempInterval.toInteger() * 60)) // Temperature Reporting
    cmd += refresh()
    
    return cmd
}

def resetBatteryReplacedDate() {
    sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
}

void parse(String description) {
    Map descMap = zigbee.parseDescriptionAsMap(description)

    if (description?.startsWith("enroll request")) {
        def result = []
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
    Map event = zigbee.getEvent(description)
    if(event) {
        switch(event.name) {
            case 'batteryVoltage':
                def pct = (event.value - minVolt()) / (maxVolt() - minVolt())
                def roundedPct = Math.round(pct * 100)
                if (roundedPct <= 0) roundedPct = 1
                def descriptionText = "${device.displayName} battery was ${Math.min(100, roundedPct)}%"
                if(infoLogging) log.info "${descriptionText}"
                sendEvent(name: 'battery', value: Math.min(100, roundedPct), unit: "%", descriptionText: descriptionText, type: 'physical')
                break
            default:
                if(infoLogging) log.info "${event.descriptionText}"
                sendEvent(name: event.name, value: event.value, unit: "°${event.unit}", descriptionText: event.descriptionText, type: 'physical')           
                break
        }
    }
}
