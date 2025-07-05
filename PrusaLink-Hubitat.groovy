metadata {
    definition(name: "PrusaLink 3D Printer", namespace: "repulsor", author: "TheCase") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        attribute "printerStatus", "string"
        attribute "jobName", "string"
        attribute "progress", "number"
        attribute "bedTemp", "number"
        attribute "nozzleTemp", "number"
        attribute "hotendStatus", "string"
        attribute "fanStatus", "string"
    }
    preferences {
        input("printerIP", "text", title: "PrusaLink Printer IP", required: true)
        input("apiKey", "text", title: "PrusaLink API Key", required: true)
        input("pollInterval", "number", title: "Poll Interval (minutes)", defaultValue: 5)
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    schedule("0 */${settings.pollInterval} * ? * *", poll)
    poll()
}

def poll() {
    refresh()
}

def refresh() {
    def params = [
        uri: "http://${settings.printerIP}/api/v1/status",
        headers: [
            "X-Api-Key": settings.apiKey
        ]
    ]
    try {
        httpGet(params) { resp ->
            log.debug "PrusaLink response: ${resp.data}"
            if (resp.status == 200) {
                parseStatus(resp.data)
            } else {
                sendEvent(name: "printerStatus", value: "Error: ${resp.status}")
            }
        }
    } catch (e) {
        sendEvent(name: "printerStatus", value: "Error: ${e.message}")
        log.error "PrusaLink error: ${e.message}"
    }
}

def parseStatus(data) {
    log.debug "parseStatus raw data: ${data}"

    // Printer state
    def status = data?.printer?.state ?: "unknown"
    sendEvent(name: "printerStatus", value: status)

    // Job info
    def jobName = data?.job?.file?.display_name ?: data?.job?.file?.name ?: "N/A"
    def progress = data?.job?.progress ?: 0
    sendEvent(name: "jobName", value: jobName)
    sendEvent(name: "progress", value: progress)

    // Temperatures
    def bedTemp = data?.printer?.temp_bed ?: 0
    def nozzleTemp = data?.printer?.temp_nozzle ?: 0
    sendEvent(name: "bedTemp", value: bedTemp)
    sendEvent(name: "nozzleTemp", value: nozzleTemp)

    // Hotend fan and print fan status (numeric, 0-100)
    def hotendFan = data?.printer?.fan_hotend != null ? data.printer.fan_hotend : -1
    def printFan = data?.printer?.fan_print != null ? data.printer.fan_print : -1
    sendEvent(name: "hotendStatus", value: hotendFan)
    sendEvent(name: "fanStatus", value: printFan)
}