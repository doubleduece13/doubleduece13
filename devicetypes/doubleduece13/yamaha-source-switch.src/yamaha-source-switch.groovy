/**
 *  SmartThings Device Handler: Yamaha Source Switch
 *    by David de Gruyl <david.degruyl@gmail.com>
 *
 *  Adapted from: Yamaha Zone
 *  by: redloro@gmail.com
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
metadata {
  definition (name: "Yamaha Source Switch", namespace: "doubleduece13", author: "David de Gruyl") {

    /**
     * List our capabilties. Doing so adds predefined command(s) which
     * belong to the capability.
     */
    capability "Switch"
    capability "Refresh"
    capability "Polling"
    command "source"
  }

  /**
   * Define the various tiles and the states that they can be in.
   * The 2nd parameter defines an event which the tile listens to,
   * if received, it tries to map it to a state.
   *
   * You can also use ${currentValue} for the value of the event
   * or ${name} for the name of the event. Just make SURE to use
   * single quotes, otherwise it will only be interpreted at time of
   * launch, instead of every time the event triggers.
   */
  tiles(scale: 2) {
    multiAttributeTile(name:"state", type:"generic", width:6, height:4) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'On', action:"source0", icon:"st.Electronics.electronics16", backgroundColor:"#79b821", nextState:"off"
        attributeState "off", label:'Off', action:"source0", icon:"st.Electronics.electronics16", backgroundColor:"#ffffff", nextState:"on"
      }
    }

    standardTile("refresh", "device.status", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh", backgroundColor:"#ffffff"
    }

    // Defines which tile to show in the overview
    main "state"

    // Defines which tile(s) to show when user opens the detailed view
    details([
      "state",
      "refresh"
    ])
  }

  preferences {
    input name: "source0", type: "text", title: "Source 1", defaultValue: "HDMI1"
  }
}

/**************************************************************************
 * The following section simply maps the actions as defined in
 * the metadata into onAction() calls.
 *
 * This is preferred since some actions can be dealt with more
 * efficiently this way. Also keeps all user interaction code in
 * one place.
 *
 */
def source0() {
  setSource(0)
}
def refresh() {
  sendCommand("<YAMAHA_AV cmd=\"GET\"><${getZone()}><Basic_Status>GetParam</Basic_Status></${getZone()}></YAMAHA_AV>")
  sendCommand("<YAMAHA_AV cmd=\"GET\"><System><Party_Mode><Mode>GetParam</Mode></Party_Mode></System></YAMAHA_AV>")
}
/**************************************************************************/

/**
 * Called every so often (every 5 minutes actually) to refresh the
 * tiles so the user gets the correct information.
 */
def poll() {
  refresh()
}

def parse(String description) {
  return
}

def setSource(id) {
  //log.debug "source: "+settings."source${id}"
  sendCommand("<YAMAHA_AV cmd=\"PUT\"><${getZone()}><Input><Input_Sel>"+settings."source${id}"+"</Input_Sel></Input></${getZone()}></YAMAHA_AV>")
  setSourceTile(settings."source${id}")
}

def setSourceTile(name) {
  sendEvent(name: "source", value: "Source: ${name}")
  for (def i = 0; i < 6; i++) {
    if (name == settings."source${i}") {
      sendEvent(name: "source${i}", value: "on")
    }
    else {
      sendEvent(name: "source${i}", value: "off")
    }
  }
}

def zone(evt) {
  /*
  * Zone Source
  */
  if (evt.Basic_Status.Input.Input_Sel.text()) {
    setSourceTile(evt.Basic_Status.Input.Input_Sel.text())
  }
}

private sendCommand(body) {
  parent.sendCommand(body)
}

private getZone() {
  return new String(device.deviceNetworkId).tokenize('|')[1]
}