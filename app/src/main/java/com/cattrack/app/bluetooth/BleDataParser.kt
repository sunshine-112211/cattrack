package com.cattrack.app.bluetooth

import com.cattrack.app.data.model.ActivityData
import com.cattrack.app.data.model.ActivityState
import com.cattrack.app.data.model.HealthData
import com.cattrack.app.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CatTrack BLE Communication Protocol
 *
 * Frame Format:
 * | Frame Header | CMD  | Data Length | Data          | Checksum |
 * | 2 bytes      | 1B   | 1 byte      | N bytes       | 1 byte   |
 * | 0xAA 0x55    | 0xXX | 0x00-0xFF   | ...           | XOR sum  |
 *
 * Commands (Device → App):
 *   0x01 - Activity Data
 *   0x02 - Health Data
 *   0x03 - Battery Level
 *   0x04 - Device Info
 *   0x05 - Realtime Sensor Data
 *   0x06 - Sync History Data
 *
 * Commands (App → Device):
 *   0x81 - Request Sync
 *   0x82 - Set Time
 *   0x83 - Start Realtime
 *   0x84 - Stop Realtime
 *   0x85 - Reboot
 *   0x86 - Start OTA
 */
@Singleton
class BleDataParser @Inject constructor() {

    companion object {
        const val FRAME_HEADER_1 = 0xAA.toByte()
        const val FRAME_HEADER_2 = 0x55.toByte()

        // Device → App Commands
        const val CMD_ACTIVITY_DATA = 0x01.toByte()
        const val CMD_HEALTH_DATA = 0x02.toByte()
        const val CMD_BATTERY = 0x03.toByte()
        const val CMD_DEVICE_INFO = 0x04.toByte()
        const val CMD_REALTIME_DATA = 0x05.toByte()
        const val CMD_SYNC_HISTORY = 0x06.toByte()

        // App → Device Commands
        const val CMD_REQUEST_SYNC = 0x81.toByte()
        const val CMD_SET_TIME = 0x82.toByte()
        const val CMD_START_REALTIME = 0x83.toByte()
        const val CMD_STOP_REALTIME = 0x84.toByte()
        const val CMD_REBOOT = 0x85.toByte()
        const val CMD_START_OTA = 0x86.toByte()
    }

    // ---- Parse Incoming Frames ----

    /**
     * Parse raw BLE data, returns ParseResult on success
     */
    fun parseFrame(data: ByteArray): ParseResult? {
        if (data.size < 5) return null
        if (data[0] != FRAME_HEADER_1 || data[1] != FRAME_HEADER_2) return null

        val cmd = data[2]
        val dataLen = data[3].toInt() and 0xFF
        if (data.size < 4 + dataLen + 1) return null

        val payload = data.copyOfRange(4, 4 + dataLen)
        val receivedChecksum = data[4 + dataLen]
        val calculatedChecksum = calculateChecksum(data, 2, 4 + dataLen)

        if (receivedChecksum != calculatedChecksum) return null

        return ParseResult(cmd, payload)
    }

    /**
     * Parse activity data packet (CMD=0x01)
     * Payload: timestamp(4B) + steps(2B) + activityState(1B) + activeMin(1B) + sleepMin(1B) + walkMin(1B) + runMin(1B)
     */
    fun parseActivityData(catId: Long, payload: ByteArray): ActivityData? {
        if (payload.size < 11) return null
        val timestamp = readUInt32(payload, 0)
        val steps = readUInt16(payload, 4)
        val stateCode = payload[6].toInt() and 0xFF
        val activeMinutes = payload[7].toInt() and 0xFF
        val sleepMinutes = payload[8].toInt() and 0xFF
        val walkMinutes = payload[9].toInt() and 0xFF
        val runMinutes = payload[10].toInt() and 0xFF
        val playMinutes = if (payload.size > 11) payload[11].toInt() and 0xFF else 0
        val axRaw = if (payload.size > 12) readInt16(payload, 12).toFloat() / 1000f else 0f
        val ayRaw = if (payload.size > 14) readInt16(payload, 14).toFloat() / 1000f else 0f
        val azRaw = if (payload.size > 16) readInt16(payload, 16).toFloat() / 1000f else 0f

        val timestampMs = timestamp * 1000L
        val date = DateUtils.formatDate(timestampMs)
        val hour = DateUtils.getHourFromTimestamp(timestampMs)
        val activityState = codeToActivityState(stateCode)

        return ActivityData(
            catId = catId,
            timestamp = timestampMs,
            date = date,
            hour = hour,
            activityState = activityState.name,
            steps = steps,
            activeMinutes = activeMinutes,
            sleepMinutes = sleepMinutes,
            restMinutes = 60 - activeMinutes - sleepMinutes,
            walkMinutes = walkMinutes,
            runMinutes = runMinutes,
            playMinutes = playMinutes,
            accelerometerX = axRaw,
            accelerometerY = ayRaw,
            accelerometerZ = azRaw
        )
    }

    /**
     * Parse health data packet (CMD=0x02)
     * Payload: timestamp(4B) + healthScore(1B) + activeLevel(1B) + sleepQuality(1B) + heartRate(1B) + calories(2B)
     */
    fun parseHealthData(catId: Long, payload: ByteArray): HealthData? {
        if (payload.size < 10) return null
        val timestamp = readUInt32(payload, 0)
        val healthScore = payload[4].toInt() and 0xFF
        val activeLevel = payload[5].toInt() and 0xFF
        val sleepQuality = payload[6].toInt() and 0xFF
        val heartRate = payload[7].toInt() and 0xFF
        val calories = readUInt16(payload, 8).toFloat() / 10f
        val hasAnomaly = if (payload.size > 10) payload[10].toInt() != 0 else false

        val timestampMs = timestamp * 1000L
        val date = DateUtils.formatDate(timestampMs)

        return HealthData(
            catId = catId,
            timestamp = timestampMs,
            date = date,
            healthScore = healthScore.coerceIn(0, 100),
            activeLevel = activeLevel.toFloat(),
            sleepQuality = sleepQuality.toFloat(),
            avgHeartRate = heartRate,
            caloriesBurned = calories,
            hasAnomaly = hasAnomaly
        )
    }

    /**
     * Parse realtime sensor packet (CMD=0x05)
     * Payload: ax(2B) + ay(2B) + az(2B) + state(1B)
     */
    fun parseRealtimeData(payload: ByteArray): RealtimeData? {
        if (payload.size < 7) return null
        val ax = readInt16(payload, 0).toFloat() / 1000f
        val ay = readInt16(payload, 2).toFloat() / 1000f
        val az = readInt16(payload, 4).toFloat() / 1000f
        val state = codeToActivityState(payload[6].toInt() and 0xFF)
        return RealtimeData(ax, ay, az, state)
    }

    // ---- Build Outgoing Commands ----

    /**
     * Build request sync command
     */
    fun buildRequestSyncCommand(): ByteArray {
        return buildFrame(CMD_REQUEST_SYNC, byteArrayOf())
    }

    /**
     * Build set time command
     * Payload: unix timestamp (4 bytes, little endian)
     */
    fun buildSetTimeCommand(timestamp: Long = System.currentTimeMillis() / 1000): ByteArray {
        val payload = ByteArray(4)
        val t = timestamp.toInt()
        payload[0] = (t and 0xFF).toByte()
        payload[1] = ((t shr 8) and 0xFF).toByte()
        payload[2] = ((t shr 16) and 0xFF).toByte()
        payload[3] = ((t shr 24) and 0xFF).toByte()
        return buildFrame(CMD_SET_TIME, payload)
    }

    /**
     * Build start realtime push command
     * Payload: interval (1 byte, seconds)
     */
    fun buildStartRealtimeCommand(intervalSeconds: Int = 1): ByteArray {
        return buildFrame(CMD_START_REALTIME, byteArrayOf(intervalSeconds.toByte()))
    }

    /**
     * Build stop realtime command
     */
    fun buildStopRealtimeCommand(): ByteArray {
        return buildFrame(CMD_STOP_REALTIME, byteArrayOf())
    }

    /**
     * Build OTA start command
     * Payload: firmware size (4B) + CRC (2B)
     */
    fun buildStartOtaCommand(firmwareSize: Int, crc: Short): ByteArray {
        val payload = ByteArray(6)
        payload[0] = (firmwareSize and 0xFF).toByte()
        payload[1] = ((firmwareSize shr 8) and 0xFF).toByte()
        payload[2] = ((firmwareSize shr 16) and 0xFF).toByte()
        payload[3] = ((firmwareSize shr 24) and 0xFF).toByte()
        payload[4] = (crc.toInt() and 0xFF).toByte()
        payload[5] = ((crc.toInt() shr 8) and 0xFF).toByte()
        return buildFrame(CMD_START_OTA, payload)
    }

    // ---- Internal Helpers ----

    private fun buildFrame(cmd: Byte, payload: ByteArray): ByteArray {
        val frame = ByteArray(4 + payload.size + 1)
        frame[0] = FRAME_HEADER_1
        frame[1] = FRAME_HEADER_2
        frame[2] = cmd
        frame[3] = payload.size.toByte()
        payload.copyInto(frame, 4)
        frame[4 + payload.size] = calculateChecksum(frame, 2, 4 + payload.size)
        return frame
    }

    private fun calculateChecksum(data: ByteArray, from: Int, to: Int): Byte {
        var sum = 0
        for (i in from until to) {
            sum = sum xor (data[i].toInt() and 0xFF)
        }
        return sum.toByte()
    }

    private fun readUInt32(data: ByteArray, offset: Int): Long {
        return ((data[offset + 3].toLong() and 0xFF) shl 24) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                (data[offset].toLong() and 0xFF)
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int {
        return ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
    }

    private fun readInt16(data: ByteArray, offset: Int): Short {
        return (((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)).toShort()
    }

    private fun codeToActivityState(code: Int): ActivityState {
        return when (code) {
            0x00 -> ActivityState.RESTING
            0x01 -> ActivityState.SLEEPING
            0x02 -> ActivityState.WALKING
            0x03 -> ActivityState.RUNNING
            0x04 -> ActivityState.PLAYING
            0x05 -> ActivityState.EATING
            else -> ActivityState.UNKNOWN
        }
    }

    // ---- Data Classes ----

    data class ParseResult(val cmd: Byte, val payload: ByteArray)

    data class RealtimeData(
        val ax: Float,
        val ay: Float,
        val az: Float,
        val state: ActivityState
    )
}
