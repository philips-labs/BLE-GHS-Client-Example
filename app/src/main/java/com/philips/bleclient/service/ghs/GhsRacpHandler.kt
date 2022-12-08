package com.philips.bleclient.service.ghs

import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.asHexString
import com.philips.bleclient.getUInt16At
import com.philips.bleclient.merge
import com.philips.bleclient.ui.ObservationLog
import com.philips.bleclient.ui.RacpLog
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

class GhsRacpHandler(val service: GenericHealthSensorServiceHandler) {

    fun useIndicationsForRACP(indicate: Boolean){
        val allPeripherals = ServiceHandlerManager.getInstance()?.getConnectedPeripherals()
        allPeripherals?.forEach {
            if (indicate) {
                service.enableIndicate(it, GenericHealthSensorServiceHandler.STORED_OBSERVATIONS_CHARACTERISTIC_UUID)
            } else {
                service.enableNotify(it, GenericHealthSensorServiceHandler.STORED_OBSERVATIONS_CHARACTERISTIC_UUID)
            }
        }
    }

    fun getNumberOfRecords() {
        racpLog("getNumberOfRecords...")
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
        racpLog("getAllRecords...")
        service.write(
            GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID,
            byteArrayOf(OP_CODE_COMBINED_REPORT, OP_ALL_RECORDS)
        )
    }

    fun deleteAllRecords() {
        racpLog("deleteAllRecords...")
        service.write(
            GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID,
            byteArrayOf(OP_CODE_DELETE_STORED_RECORDS, OP_ALL_RECORDS)
        )
    }

    fun deleteRecordsAbove(recordNumber: Int) {
        racpLog("deleteRecordsAbove $recordNumber...")
        val parser = BluetoothBytesParser()
        parser.setIntValue(recordNumber, BluetoothBytesParser.FORMAT_UINT32)
        val sendBytes = listOf(
            byteArrayOf(
                OP_CODE_DELETE_STORED_RECORDS,
                OP_GREATER_THAN_OR_EQUAL,
                OP_FILTER_TYPE_VALUE_REC_NUM), parser.value).merge()
        service.write(GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID, sendBytes)
    }

    fun getRecordsAbove(recordNumber: Int) {
        racpLog("getRecordsAbove $recordNumber...")
        val parser = BluetoothBytesParser()
        parser.setIntValue(recordNumber, BluetoothBytesParser.FORMAT_UINT32)
        val sendBytes = listOf(
            byteArrayOf(
                OP_CODE_COMBINED_REPORT,
                OP_GREATER_THAN_OR_EQUAL,
                OP_FILTER_TYPE_VALUE_REC_NUM), parser.value).merge()
        service.write(GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID, sendBytes)
    }

    fun abortGetRecords() {
        service.write(
            GenericHealthSensorServiceHandler.RACP_CHARACTERISTIC_UUID,
            byteArrayOf(OP_CODE_ABORT, OP_NULL)
        )
    }

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Received RACP Response Bytes: <${value.asHexString()}> for peripheral: ${peripheral.address}")
        when(value.first()) {
            OP_CODE_RESPONSE_NUMBER_STORED_RECORDS -> handleResponseNumberStoredRecords(peripheral, value)
            OP_CODE_DELETE_STORED_RECORDS -> handleResponseDeleteStoredRecords(peripheral, value)
            OP_CODE_RESPONSE_COMBINED_REPORT -> handleResponseCombinedReport(peripheral, value)
            OP_CODE_RESPONSE_CODE -> handleReponseCode(peripheral, value)
        }
    }

    fun handleReponseCode(peripheral: BluetoothPeripheral, value: ByteArray) {
        if (value.size != 4){
            racpLog("Incorrect RACP response received.")
//            ObservationLog.log("Incorrect RACP response received.")
        } else {
            when (value.last()){
                RESPONSE_CODE_SUCCESS -> {
                    racpLog("RESPONSE_CODE_SUCCESS received.")
                    service.onRacpAbortCompleted(peripheral.address)
                }
                RESPONSE_CODE_NO_RECORDS -> {
                    racpLog("RESPONSE_CODE_NO_RECORDS received.")
                    handleResponseNoRecordsFound(peripheral)
                }
                RESPONSE_CODE_ABORT_UNSUCCESSFUL -> {
                    racpLog("RESPONSE_CODE_ABORT_UNSUCCESSFUL received.")
                    service.onRacpAbortError(peripheral.address, value.last())
                }
                RESPONSE_CODE_INVALID_OPERAND -> racpLog("RESPONSE_CODE_INVALID_OPERAND received.")
                RESPONSE_CODE_INVALID_OPERATOR -> racpLog("RESPONSE_CODE_INVALID_OPERATOR received.")
                RESPONSE_CODE_OPERAND_UNSUPPORTED -> racpLog("RESPONSE_CODE_OPERAND_UNSUPPORTED received.")
                RESPONSE_CODE_OPERATOR_UNSUPPORTED -> racpLog("RESPONSE_CODE_OPERATOR_UNSUPPORTED received.")
                RESPONSE_CODE_OP_CODE_UNSUPPORTED -> racpLog("RESPONSE_CODE_OP_CODE_UNSUPPORTED received.")
                RESPONSE_CODE_PROCEDURE_NOT_COMPLETED -> racpLog("RESPONSE_CODE_PROCEDURE_NOT_COMPLETED received.")
            }
        }

    }

    private fun racpLog(message: String) {
        RacpLog.log(message)
        Timber.i(message)
    }

    fun handleResponseNumberStoredRecords(peripheral: BluetoothPeripheral, value: ByteArray) {
        val numberOfRecords = value.getUInt16At(2)
        service.onNumberOfStoredRecordsResponse(peripheral.address, numberOfRecords)
    }


    fun handleResponseDeleteStoredRecords(peripheral: BluetoothPeripheral, value: ByteArray) {
        val numberOfRecords = value.getUInt16At(2)
        service.onDeleteStoredRecordsResponse(peripheral.address, numberOfRecords)
    }

    fun handleResponseCombinedReport(peripheral: BluetoothPeripheral, value: ByteArray) {
        val numberOfRecords = value.getUInt16At(2)
        service.onNumberOfStoredRecordsRetrieved(peripheral.address, numberOfRecords)
    }

    fun handleResponseNoRecordsFound(peripheral: BluetoothPeripheral) {
        val numberOfRecords = 0
        service.onNumberOfStoredRecordsRetrieved(peripheral.address, numberOfRecords)
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
        private const val RESPONSE_CODE_OP_CODE_UNSUPPORTED = 0x02.toByte()
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
    private fun ByteArray.racpSuccessResponse(): Boolean = (size == 4) && (this.last() == RESPONSE_CODE_SUCCESS)

}