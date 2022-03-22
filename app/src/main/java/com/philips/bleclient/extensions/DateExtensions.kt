package com.philips.bleclient.extensions


import kotlinx.datetime.toJavaLocalDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

enum class GhsTimestampFlags(override val bit: Long) : Flags {
    isTickCounter(1 shl 0),
    isUTC(1 shl 1),
    isMillisecondsPresent(1 shl 2),
    isHundredthsMicroseconds(1 shl 3),
    isTZPresent(1 shl 4),
    isDSTPresent(1 shl 5),
    isCurrentTimeline(1 shl 6),
    reserved_1(1 shl 7);
}

const val MILLIS_IN_15_MINUTES = 900000L
const val UTC_TO_UNIX_EPOCH_MILLIS = 946684800000L

/*
 * Create a binary representation of the receiver based on the timestamp flags passed in.
 *
 * @returns bytes that are compliant with the GHS Time specification
 */
fun Date.asGHSBytes(timestampFlags: GhsTimestampFlags): ByteArray {
    return byteArrayOf(0)
//    var hexString = this.toUINT8().toString(16).toUpperCase(Locale.ROOT)
//    if (this.toUINT8() < 16) hexString = "0$hexString"
//    return hexString
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

    val timecounter =
        (if (timestampFlags.hasFlag(GhsTimestampFlags.isMillisecondsPresent)) {
            this
        } else if (timestampFlags.hasFlag(GhsTimestampFlags.isHundredthsMicroseconds)) {
            this / 10
        } else {
            this * 1000
        })  + UTC_TO_UNIX_EPOCH_MILLIS

//    if (timestampFlags.hasFlag(GhsTimestampFlags.isTickCounter)) {
//        // TODO What sort of "time" represents the tick counter, or null... or throw an exception
//    } else {
        val timeOffset = if (timestampFlags.hasFlag(GhsTimestampFlags.isTZPresent) ||
                            timestampFlags.hasFlag(GhsTimestampFlags.isDSTPresent)) offset * ( 15 * 60 * 1000)
                        else 0
        return (timecounter + timeOffset).millisAsLocalDateTime()
//    }

}

fun kotlinx.datetime.LocalDateTime.asDisplayString() : String {
    return this.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-mm-dd HH:MM:SS"))
}