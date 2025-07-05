metadata {
    definition(name: "PrusaLink Printer", namespace: "repulsor", author: "TheCase") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        attribute "printerStatus", "string"
        attribute "jobName", "string"
        attribute "progress", "number"
        attribute "bedTemp", "number"
        attribute "nozzleTemp", "number"
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

    def status = data?.printer?.state ?: "unknown"
    sendEvent(name: "printerStatus", value: status)

    // PrusaLink's response does not include job name or progress in this structure
    sendEvent(name: "jobName", value: "N/A")
    sendEvent(name: "progress", value: 0)

    def bedTemp = data?.printer?.temp_bed ?: 0
    def nozzleTemp = data?.printer?.temp_nozzle ?: 0
    sendEvent(name: "bedTemp", value: bedTemp)
    sendEvent(name: "nozzleTemp", value: nozzleTemp)
}