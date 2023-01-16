package com.philips.bleclient.extensions


import android.os.SystemClock
import com.philips.bleclient.asBitmask
import com.philips.bleclient.asHexString
import com.philips.bleclient.merge
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.GattStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

enum class TimestampFlags(override val bit: Long) : Flags {
    zero((0 shl 0).toLong()),
    isTickCounter((1 shl 0).toLong()),
    isUTC((1 shl 1).toLong()),
    isHundredthsMilliseconds((1 shl 2).toLong()),
    isMilliseconds((1 shl 3).toLong()),
    isTZPresent((1 shl 4).toLong()),
    isCurrentTimeline((1 shl 5).toLong()),
    reserved_1((1 shl 6).toLong()),
    reserved_2((1 shl 7).toLong());

    companion object {
        // This "global" holds the flags used to send out observations
//        var currentFlags: BitMask = BitMask(TimestampFlags.isMilliseconds.bit)
//            .plus(TimestampFlags.isTZPresent)
//            .plus(TimestampFlags.isCurrentTimeline)

        var currentFlags: BitMask = BitMask(0)

        fun setLocalFlags() {
            currentFlags = currentFlags.unset(isUTC)
            currentFlags = currentFlags.unset(isTZPresent)
            currentFlags = currentFlags.unset(isTickCounter)
        }

        fun setLocalWithOffsetFlags() {
            currentFlags = currentFlags.unset(isUTC)
            currentFlags = currentFlags.set(isTZPresent)
            currentFlags = currentFlags.unset(isTickCounter)
        }

        fun setUtcOnlyFlags() {
            currentFlags = currentFlags.set(isUTC)
            currentFlags = currentFlags.unset(isTZPresent)
            currentFlags = currentFlags.unset(isTickCounter)
        }

        fun setUtcWithOffsetFlags() {
            currentFlags = currentFlags.set(isUTC)
            currentFlags = currentFlags.set(isTZPresent)
            currentFlags = currentFlags.unset(isTickCounter)
        }

        fun setTickCounterFlags() {
            currentFlags = currentFlags.unset(isUTC)
            currentFlags = currentFlags.unset(isTZPresent)
            currentFlags = currentFlags.set(isTickCounter)
        }
    }
}

fun BitMask.isMilliseconds(): Boolean {
    return (this hasFlag TimestampFlags.isMilliseconds) and !(this hasFlag TimestampFlags.isHundredthsMilliseconds)
}

fun BitMask.isHundredMilliseconds(): Boolean {
    return !(this hasFlag TimestampFlags.isMilliseconds) and (this hasFlag TimestampFlags.isHundredthsMilliseconds)
}

fun BitMask.isSeconds(): Boolean {
    return !(this hasFlag TimestampFlags.isMilliseconds) and !(this hasFlag TimestampFlags.isHundredthsMilliseconds)
}

fun BitMask.isHundredthsMicroseconds(): Boolean {
    return !(this hasFlag TimestampFlags.isMilliseconds) and !(this hasFlag TimestampFlags.isMilliseconds)
}


fun BitMask.asTimestampFlagsString(): String {
    val ticksOrTime = if (this hasFlag TimestampFlags.isTickCounter) "Ticks" else "Time"
    val utcOrLocal = if (this hasFlag TimestampFlags.isUTC) "UTC" else "Local"
    val millsOrSecs = if (this.isMilliseconds()) "Millis"
    else if (this.isHundredMilliseconds())  "100ths millis"
    else if (this.isSeconds()) "seconds"
    else if(this.isHundredthsMicroseconds()) "100th microsecods"
    else "unknown resolution"
    val hasTZ = if (this hasFlag TimestampFlags.isTZPresent) "TZ" else "No TZ"
    val current = if (this hasFlag TimestampFlags.isCurrentTimeline) "Current" else "Not Current"
    return "Value: ${value.toByte().asHexString()} : $ticksOrTime : $utcOrLocal : $millsOrSecs : $hasTZ : $current timeline"
}

const val MILLIS_IN_15_MINUTES = 900000
// Magic number 946684800000 is the millisecond offset from 1970 Epoch to Y2K Epoch
const val UTC_TO_UNIX_EPOCH_MILLIS = 946684800000L

enum class Timesource(val value: Int) {
    Unknown(0),
    NTP(1),
    GPS(2),
    RadioTimeSignal(3),
    Manual(4),
    Atomic(5),
    CellularNetwork(6),
    None(7);

    override fun toString(): String {
        return when (value) {
            Unknown.value -> "Unknown"
            NTP.value -> "NTP"
            GPS.value -> "GPS"
            RadioTimeSignal.value -> "Radio Time Signal"
            Manual.value -> "Manual"
            Atomic.value -> "Atomic Clock"
            CellularNetwork.value -> "Cellular Network"
            None.value -> "None"
            else -> "Undefined"
        }
    }

    companion object {
        // This "global" holds the current time source used to send out observations
        var currentSource: Timesource = CellularNetwork

        fun value(value: Int): Timesource {
            return when (value) {
                Unknown.value -> Unknown
                NTP.value -> NTP
                GPS.value -> GPS
                RadioTimeSignal.value -> RadioTimeSignal
                Manual.value -> Manual
                Atomic.value -> Atomic
                CellularNetwork.value -> CellularNetwork
                None.value -> None
                else -> Unknown
            }
        }
    }

}

/*
 * Create a binary representation of the receiver based on the timestamp flags set in TimestampFlags.currentFlags
 * The flags and bytes are in the GHS specification in section 2.5.3.2 (as of the 0.5 draft)
 *
 * @returns bytes that are compliant with the GHS Time specification
 */
fun Date.asGHSBytes(): ByteArray {
    return asGHSBytes(TimestampFlags.currentFlags)
}

fun Long.asGHSTimeValue(): ByteArray {
    val millParser = BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN)
    millParser.setLong(this, BluetoothBytesParser.FORMAT_UINT48)
    return millParser.value
}

fun Long.asGHSTicks(flags: BitMask): ByteArray {
    val newFlags = BitMask(TimestampFlags.isTickCounter.bit)
    if (flags hasFlag TimestampFlags.isMilliseconds) newFlags.set(TimestampFlags.isMilliseconds)
    if (flags hasFlag TimestampFlags.isHundredthsMilliseconds) newFlags.set(TimestampFlags.isHundredthsMilliseconds)
    return listOf(
        byteArrayOf(newFlags.value.toByte()),
        this.asGHSTimeValue(),
        byteArrayOf(0, 0)
    ).merge()

}

fun ByteArray.parseSTSDate() : Date? {
    val writeFlags = this[0].asBitmask()
    return if (writeFlags.hasFlag(TimestampFlags.isTickCounter)) {
        null
    } else {
        var ticks = this[1].toUByte().toLong() +
                this[2].toUByte().toLong().shl(8) +
                this[3].toUByte().toLong().shl(16) +
                this[4].toUByte().toLong().shl(24) +
                this[5].toUByte().toLong().shl(32) +
                this[6].toUByte().toLong().shl(40)
        Timber.i("Epoch millis Value: Unix: ${ticks + UTC_TO_UNIX_EPOCH_MILLIS} Y2K: $ticks")
        val timeSource = this[7]
        val offset = this[8] * MILLIS_IN_15_MINUTES
        val milliScale = if (writeFlags.hasFlag(TimestampFlags.isMilliseconds)) 1L else 1000L
        val scaledTicks = writeFlags.convertY2KScaledToUTCEpochMillis(ticks)
        Timber.i("UTC Epoch Millis:$scaledTicks Offset: $offset")
        Date(scaledTicks + offset)
    }
}

/*
 * Convert the value passed in from a Y2K epoch value to a Unix Epoch (1970) value in millisecods
 * The receiver bit mask encodes the resolution of the value (seconds, millis, hundred millis,
 * hundred microseconds as defined by the spec)
 */
fun BitMask.convertY2KScaledToUTCEpochMillis(value: Long): Long {
    return (if (isSeconds()) value * 1000L
    else if (isMilliseconds()) value
    else if(isHundredMilliseconds()) value * 100L
    else if(isHundredthsMicroseconds()) value / 10L
    else value) + UTC_TO_UNIX_EPOCH_MILLIS
}

// TODO: Cleanup now that flags have changed
fun BitMask.getTimeResolutionScaledValue(millis: Long): Long {
    return if (isSeconds()) millis / 1000L
        else if (isMilliseconds()) millis
        else if(isHundredMilliseconds()) millis / 10L
        else if(isHundredthsMicroseconds()) millis * 10L
        else millis
}

fun Date.asGHSBytes(timestampFlags: BitMask): ByteArray {

    val currentTimeMillis = System.currentTimeMillis()
    val isTickCounter = timestampFlags hasFlag TimestampFlags.isTickCounter

    var millis = if (isTickCounter) SystemClock.elapsedRealtime() else epoch2000mills()
    Timber.i("Epoch millis Value: Unix: ${millis + UTC_TO_UNIX_EPOCH_MILLIS} Y2K: $millis")

    if (!(timestampFlags hasFlag TimestampFlags.isTickCounter)) {
        // Used if the clock is reporting local time, not UTC time. Get UTC offset and add it to the milliseconds reported for local time
        val utcOffsetMillis = if (timestampFlags hasFlag TimestampFlags.isUTC) 0L else java.util.TimeZone.getDefault().getOffset(currentTimeMillis).toLong()
        millis += utcOffsetMillis
    }

    // Write the flags byte
    Timber.i("Add Flag: ${timestampFlags.asTimestampFlagsString()}")

    val scaledTicks = timestampFlags.getTimeResolutionScaledValue(millis)
    Timber.i("Scaled ticks: $scaledTicks")

    var offsetUnits = 0

    if (!isTickCounter) {
        val calendar = Calendar.getInstance(Locale.getDefault());
        val timeZoneMillis = if (timestampFlags hasFlag TimestampFlags.isTZPresent) calendar.get(Calendar.ZONE_OFFSET) else 0
        val dstMillis =if (timestampFlags hasFlag TimestampFlags.isTZPresent) calendar.get(Calendar.DST_OFFSET) else 0
        offsetUnits = (timeZoneMillis + dstMillis) / MILLIS_IN_15_MINUTES
        Timber.i("Add Offset Value: $offsetUnits")
    }

    val millParser = BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN)
    // millParser.setLong(scaledTicks)
    millParser.setLong(0L, BluetoothBytesParser.FORMAT_UINT48)

    return listOf(
        byteArrayOf(timestampFlags.value.toByte()),
        millParser.value,
        byteArrayOf(Timesource.currentSource.value.toByte(), offsetUnits.toByte())
    ).merge()

}

fun LocalDateTime.toDate(): Date {
    return Date.from(atZone(ZoneId.systemDefault()).toInstant())
}

fun Date.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault())
}

/*
 * Return the Epoch Y2K milliseconds (used by GHS)
 */
fun Date.epoch2000mills(): Long {
    return time - UTC_TO_UNIX_EPOCH_MILLIS
}

fun Long.asKotlinLocalDateTime(timestampFlags: BitMask, offset: Int): kotlinx.datetime.LocalDateTime {


    val timecounter = timestampFlags.convertY2KScaledToUTCEpochMillis(this)

    Timber.i("asKotlinLocalDateTime value: $this epoch millis: $timecounter offset: $offset flags: ${timestampFlags.asTimestampFlagsString()}")

    if (timestampFlags.hasFlag(TimestampFlags.isTickCounter)) {
        // TODO What sort of "time" represents the tick counter, or null... or throw an exception
        throw RuntimeException("Attempt to convert a tick counter timestamp value into calendar datetime")
    } else {
        val timeOffset = if (timestampFlags.hasFlag(TimestampFlags.isTZPresent)) offset * MILLIS_IN_15_MINUTES else 0
        val result = if (timestampFlags.hasFlag(TimestampFlags.isUTC)) {
            // We received a UTC Time so need to add our UTC Offset
            var tz = TimeZone.UTC
            if (timeOffset != 0) {
                val zoneOffet = ZoneOffset.ofTotalSeconds(timeOffset / 1000)
                tz = TimeZone.currentSystemDefault()
            }
            timecounter.millisAsUTCDateTime()
        } else {
            (timecounter + timeOffset).millisAsLocalDateTime()
        }
        return result
    }
}

fun kotlinx.datetime.LocalDateTime.asDisplayString() : String {
    return this.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
}