metadata {
    definition(name: "PrusaLink 3D Printer", namespace: "repulsor", author: "TheCase") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        attribute "printerState", "string"
        attribute "progress", "number"
        attribute "bedTemp", "number"
        attribute "nozzleTemp", "number"
        attribute "bedTarget", "number"
        attribute "nozzleTarget", "number"
        attribute "hotendFan", "string"
        attribute "printFan", "string"
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
                sendEvent(name: "printerState", value: "Error: ${resp.status}")
            }
        }
    } catch (e) {
        sendEvent(name: "printerState", value: "Error: ${e.message}")
        log.error "PrusaLink error: ${e.message}"
    }
}

def parseStatus(data) {
    log.debug "parseStatus raw data: ${data}"

    // Printer state
    def status = data?.printer?.state ?: "unknown"
    sendEvent(name: "printerState", value: status)

    // Job progress (jobName removed, not present in status output)
    def progress = data?.job?.progress ?: 0
    sendEvent(name: "progress", value: progress)

    // Temperatures
    def bedTemp = data?.printer?.temp_bed ?: 0
    def nozzleTemp = data?.printer?.temp_nozzle ?: 0
    def bedTarget = data?.printer?.target_bed ?: 0
    def nozzleTarget = data?.printer?.target_nozzle ?: 0
    sendEvent(name: "bedTemp", value: bedTemp)
    sendEvent(name: "nozzleTemp", value: nozzleTemp)
    sendEvent(name: "bedTarget", value: bedTarget)
    sendEvent(name: "nozzleTarget", value: nozzleTarget)

    // Hotend fan and print fan status
    def hotendFan = data?.printer?.fan_hotend != null ? data.printer.fan_hotend : -1
    def printFan = data?.printer?.fan_print != null ? data.printer.fan_print : -1
    sendEvent(name: "hotendFan", value: hotendFan)
    sendEvent(name: "printFan", value: printFan)
}