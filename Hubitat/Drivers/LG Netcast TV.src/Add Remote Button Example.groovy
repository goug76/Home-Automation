public static String version()      {  return "v1.0.0"  }
public static String name()         {  return "LG Netcast TV"  }

metadata {
	definition (name: name(), namespace: "goug76", author: "John Goughenour", cstHandler: true) {
		capability "Audio Volume"
		capability "Switch"
		capability "TV"
        capability "Refresh"
        capability "Configuration"
        
        attribute "session", "string"
        attribute "maxVolume", "number"
        attribute "minVolume", "number"
        attribute "inputSource", "string"
        attribute "inputLabel", "string"
        attribute "majorChannel", "string"
        attribute "minorChannel", "string"
        attribute "channelName", "string"
        attribute "currentProgram", "string"
        attribute "model", "string"
        
        command "showKey"
        command "exampleButton"  //Add the Command for the button i.e. Up Arrow
	}


	preferences {
        //CODE
  }
}
// Then create the method for the button. 
def exampleButton() {
    if (detailedLog) log.debug "Executing 'exampleButton'"
	sendCommand(27)
    refresh()
}
