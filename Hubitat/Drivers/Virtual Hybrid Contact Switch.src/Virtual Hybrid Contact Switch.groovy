/**
 *  Virtual Hybrid Contact Switch
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
public static String name()         {  return "Virtual Hybrid Contact Switch"  }

metadata {
    definition (name: name(), namespace: "goug76", author: "John Goughenour") {
    capability "Sensor"
    capability "Contact Sensor"
    capability "Switch"
    
    command "open"
    command "close"
    }
  preferences {
        input name: "autoOff", type: "number", title: "<b>Auto Off</b>", 
            description: "Automatically turn off device after x many seconds </br>Default: 0 (Disabled)",
            defaultValue: 0, required: false
        input name:"about", type: "text", title: "<b>About Driver</b>", 
            description: "A hybrid contact sensor/swtich that can be used in Hubitat to trigger Alexa Routines. <p style=\"text-align:center\"></br><strong>John Goughenour</strong> (goug76)</br>${name()}<br/><em>${version()}</em></p>"
  }
}

def open(){
  sendEvent(name: "contact", value: "open")
  sendEvent(name: "switch", value: "on")
}

def close(){
  sendEvent(name: "contact", value: "closed")
  sendEvent(name: "switch", value: "off")
}

def on(){
  sendEvent(name: "switch", value: "on")
  sendEvent(name: "contact", value: "open")
  if( autoOff > 0 ) runIn (autoOff, off)
}

def off(){
  sendEvent(name: "switch", value: "off")
  sendEvent(name: "contact", value: "closed")
}

def installed(){
  initialize()
}

def updated(){
  initialize()
}

def initialize(){
  sendEvent(name: "switch", value: "off")
  sendEvent(name: "contact", value: "closed")
  
}
