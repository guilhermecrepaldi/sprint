package com.strava_matematica.network

import com.strava_matematica.model.PenEvent

class TelemetrySocket {
    fun connect(sessionId: String) {
        // Future: OkHttp WebSocket /api/telemetry/stream.
    }

    fun send(event: PenEvent) {
        // Future: stream low-latency stylus telemetry.
    }

    fun close() = Unit
}
