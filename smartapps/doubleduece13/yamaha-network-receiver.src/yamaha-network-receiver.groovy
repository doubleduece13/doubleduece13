/**
 *  SmartThings SmartApp: Yamaha Network Receiver
 *
 *  Author: redloro@gmail.com
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
 *  https://github.com/PSeitz/yamaha-nodejs
 */
definition(
  name: "Yamaha Network Receiver",
  namespace: "doubleduece13",
  author: "david.degruyl@gmail.com",
  description: "Yamaha SmartApp",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/redloro/smartthings/master/images/yamaha-receiver.png",
  iconX2Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/yamaha-receiver.png",
  iconX3Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/yamaha-receiver.png",
  singleInstance: true
)

preferences {
  section("SmartThings Hub") {
    input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  }
  section("Yamaha Receiver") {
    input name: "receiverIp", type: "text", title: "IP", required: true
    input name: "receiverZones", type: "enum", title: "Zones", required: true, multiple: true, options: ["Main_Zone","Zone_2","Zone_3","Zone_4"]
    input name: "receiverInputs", type: "enum", title: "Inputs", required: true, multiple: true, options: 
    	["AUDIO1", "AUDIO2", "AV1", "AV2", "AV3", "AV4", "AV5", "AV6", "Bluetooth", "DOCK", "HDMI1", "HDMI2", "HDMI3", "HDMI4", 
         "HDMI5", "iPod", "MULTI CH", "SIRIUS", "TUNER", "UAW", "V-AUX", "USB", "Pandora"]
  }
}

def installed() {
  subscribeToEvents()
}

def subscribeToEvents() {
  subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def updated() {
  addChildDevices()
}

def uninstalled() {
  removeChildDevices()
}

def lanResponseHandler(evt) {
  def map = stringToMap(evt.stringValue)

  //verify that this message is from Yamaha Receiver IP
  if (!map.ip || map.ip != convertIPtoHex(settings.receiverIp)) {
    return
  }

  def headers = getHttpHeaders(map.headers);
  def body = getHttpBody(map.body);
  //log.trace "Headers: ${headers}"
  //log.trace "Body: ${body}"
  
  updateZoneDevices(body.children()[0])
}

private updateZoneDevices(evt) {
  //log.debug "updateZoneDevices: ${evt.toString()}"
  if (evt.name() == "System") {
    //log.debug "Update all zones"
    childDevices*.zone(evt)
    return
  }

  def zonedevice = getChildDevice("yamaha|${evt.name()}")
  if (zonedevice) {
    zonedevice.zone(evt)
  }
}

private addChildDevices() {
  // add yamaha device
  settings.receiverZones.each { 
    def deviceId = 'yamaha|'+it
    if (!getChildDevice(deviceId)) {
      addChildDevice("redloro-smartthings", "Yamaha Zone", deviceId, hostHub.id, ["name": it, label: "Yamaha: "+it, completedSetup: true])
      log.debug "Added zone device: ${deviceId}"
    } 
  }
//  settings.receiverInputs.each { 
//    def deviceId = 'yamaha_input|Main_Zone|'+it
//    if (!getChildDevice(deviceId)) {
//      addChildDevice("doubleduece13", "Yamaha Source Switch", deviceId, hostHub.id, ["name": it, label: "Yamaha Input: "+it, "source0": it, "completedSetup": true])
//      log.debug "Added zone device: ${deviceId}"
//    } 
//  }

  childDevices*.refresh()
}

private removeChildDevices() {
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

private sendCommand(body) {
  //log.debug "Yamaha Network Receiver send command: ${body}"
  
  def hubAction = new physicalgraph.device.HubAction(
      headers: [HOST: getReceiverAddress()],
      method: "POST",
      path: "/YamahaRemoteControl/ctrl",
      body: body
  )
  sendHubCommand(hubAction)
}

private getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    obj = new XmlSlurper().parseText(new String(body.decodeBase64()))
  }
  return obj
}

private getReceiverAddress() {
  return settings.receiverIp + ":80"
}

private String convertIPtoHex(ipAddress) { 
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() ).toUpperCase()
  return hexport
}