package com.philips.bleclient.service.ghs

import com.philips.bleclient.asHexString
import com.philips.bleclient.getUInt16At
import com.philips.bleclient.merge
import com.philips.bleclient.ui.ObservationLog
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

class GhsRacpHandler(val service: GenericHealthSensorServiceHandler) {

    fun getNumberOfRecords() {
        service.write(
            GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID,
            byteArrayOf(OP_CODE_NUMBER_STORED_RECORDS, OP_ALL_RECORDS)
        )
    }

    fun getNumberOfRecordsGreaterThan(recordNumber: Int) {
        val parser = BluetoothBytesParser()
        parser.setIntValue(recordNumber, BluetoothBytesParser.FORMAT_UINT32)
        val sendBytes = listOf(
            byteArrayOf(
                OP_CODE_NUMBER_STORED_RECORDS,
                OP_GREATER_THAN_OR_EQUAL,
                OP_FILTER_TYPE_VALUE_REC_NUM), parser.value).merge()
        service.write(GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID, sendBytes)
    }

    fun getAllRecords() {
        service.write(
            GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID,
            byteArrayOf(OP_CODE_COMBINED_REPORT, OP_ALL_RECORDS)
        )
    }

    fun getRecordsAbove(recordNumber: Int) {
        val parser = BluetoothBytesParser()
        parser.setIntValue(recordNumber, BluetoothBytesParser.FORMAT_UINT32)
        val sendBytes = listOf(
            byteArrayOf(
                OP_CODE_COMBINED_REPORT,
                OP_GREATER_THAN_OR_EQUAL,
                OP_FILTER_TYPE_VALUE_REC_NUM), parser.value).merge()
        service.write(GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID, sendBytes)
    }

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Received RACP Response Bytes: <${value.asHexString()}> for peripheral: ${peripheral.address}")
        when(value.first()) {
            OP_CODE_RESPONSE_NUMBER_STORED_RECORDS -> handleResponseNumberStoredRecords(peripheral, value)
            OP_CODE_RESPONSE_COMBINED_REPORT -> handleResponseCombinedReport(peripheral, value)
        }
    }

    fun handleResponseNumberStoredRecords(peripheral: BluetoothPeripheral, value: ByteArray) {
        val numberOfRecords = value.getUInt16At(2)
        Timber.i("RACP Number of stored records: $numberOfRecords for peripheral: ${peripheral.address}")
        ObservationLog.log("RACP: Number of stored records $numberOfRecords ")
    }

    fun handleResponseCombinedReport(peripheral: BluetoothPeripheral, value: ByteArray) {
        val numberOfRecords = value.getUInt16At(2)
        Timber.i("RACP Number of retrieved records: $numberOfRecords for peripheral: ${peripheral.address}")
        ObservationLog.log("RACP: Number of retrieved records $numberOfRecords ")
    }

    companion object {
        /*
         * RACP Operator Code Values
         */
        private const val OP_CODE_REPORT_STORED_RECORDS = 0x01.toByte()
        private const val OP_CODE_DELETE_STORED_RECORDS = 0x02.toByte()
        private const val OP_CODE_ABORT = 0x03.toByte()
        private const val OP_CODE_NUMBER_STORED_RECORDS = 0x04.toByte()
        private const val OP_CODE_COMBINED_REPORT = 0x07.toByte()

        private const val OP_CODE_RESPONSE_NUMBER_STORED_RECORDS = 0x05.toByte()
        private const val OP_CODE_RESPONSE_CODE = 0x06.toByte()
        private const val OP_CODE_RESPONSE_COMBINED_REPORT = 0x08.toByte()

        private const val OP_FILTER_TYPE_VALUE_REC_NUM = 0x01.toByte()
        private const val OP_FILTER_TYPE_VALUE_TIME = 0x02.toByte()

        /*
         * RACP Operator Values
         */
        private const val OP_NULL = 0x0.toByte()
        private const val OP_ALL_RECORDS = 0x01.toByte()
        private const val OP_LESS_THAN_OR_EQUAL = 0x02.toByte()
        private const val OP_GREATER_THAN_OR_EQUAL = 0x03.toByte()
        private const val OP_WITHIN_RANGE = 0x04.toByte()
        private const val OP_FIRST_RECORD = 0x05.toByte()
        private const val OP_LAST_RECORD = 0x06.toByte()

        /*
         * Response Code values associated with Op Code 0x06
         */
        private const val RESPONSE_CODE_SUCCESS = 0x01.toByte()
        private const val RESPONSE_CODE_OP_CODE_UNSUPPOERTED = 0x02.toByte()
        private const val RESPONSE_CODE_INVALID_OPERATOR = 0x03.toByte()
        private const val RESPONSE_CODE_OPERATOR_UNSUPPORTED = 0x04.toByte()
        private const val RESPONSE_CODE_INVALID_OPERAND = 0x05.toByte()
        private const val RESPONSE_CODE_NO_RECORDS = 0x06.toByte()
        private const val RESPONSE_CODE_ABORT_UNSUCCESSFUL = 0x07.toByte()
        private const val RESPONSE_CODE_PROCEDURE_NOT_COMPLETED = 0x08.toByte()
        private const val RESPONSE_CODE_OPERAND_UNSUPPORTED = 0x09.toByte()
    }

    private fun ByteArray.racpOpCode(): Byte = this[0]
    private fun ByteArray.racpOperator(): Byte = this[1]

}