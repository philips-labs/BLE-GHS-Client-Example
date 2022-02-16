package com.philips.bleclient.extensions


import java.time.LocalDateTime
import java.time.ZoneId
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
