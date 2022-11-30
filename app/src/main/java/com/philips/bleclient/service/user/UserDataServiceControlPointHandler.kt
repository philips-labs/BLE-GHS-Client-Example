package com.philips.bleclient.service.user

import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.asHexString
import com.philips.bleclient.merge
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.ghs.GhsRacpHandler
import com.philips.bleclient.ui.UsersLog
import com.welie.blessed.BluetoothBytesParser
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

enum class UserDataControlPointOpCode(val value: Byte) {
    RegisterNewUser(1),
    UserConsent(2),
    DeleteUserData(3),
    ListAllUsers(4),
    DeleteUser(5),
    Unknown(0xFF.toByte());

    override fun toString(): String {
        return when (value) {
            RegisterNewUser.value -> "Unknown"
            UserConsent.value -> "User Consent"
            DeleteUserData.value -> "Delete User Data"
            ListAllUsers.value -> "List All Users"
            DeleteUser.value -> "Delete User"
            else -> "Undefined"
        }
    }

    companion object {
        fun value(value: Byte): UserDataControlPointOpCode {
            return when (value) {
                RegisterNewUser.value -> RegisterNewUser
                UserConsent.value -> UserConsent
                DeleteUserData.value -> DeleteUserData
                ListAllUsers.value -> ListAllUsers
                DeleteUser.value -> DeleteUser
                else -> Unknown
            }
        }

    }
}

class UserDataServiceControlPointHandler(val service: UserDataServiceHandler) {

    fun handleBytes(peripheral: BluetoothPeripheral, value: ByteArray) {
        Timber.i("Received Control Point Response Bytes: <${value.asHexString()}> for peripheral: $peripheral")
        if (value.isNotEmpty()) {
            if (value.first() == RESPONSE_CODE_OP_CODE) {
                when(value.responseRequestOpCode()) {
                    UserDataControlPointOpCode.RegisterNewUser.value -> newUserRegistered(value)
                    UserDataControlPointOpCode.UserConsent.value -> userConsentResult(value)
                    UserDataControlPointOpCode.DeleteUser.value -> deleteUserResult(value)
                }
            } else {
                Timber.i("ERROR: UDS Control Point received non 0x20 Response Op Code bytes: <${value.asHexString()}> for peripheral: $peripheral")
            }
        }
    }


    fun newUserWithConsentCode(consentCode: Int) {
        val parser = BluetoothBytesParser()
        parser.setUInt8(UserDataControlPointOpCode.RegisterNewUser.value.toInt())
        parser.setUInt16(consentCode)
        val sendBytes = parser.value
        Timber.i("Writing new user with consent command bytes: ${sendBytes.asFormattedHexString()}")
        service.write(UserDataServiceHandler.UDS_CONTROL_POINT_CHARACTERISTIC_UUID, sendBytes)
    }

    fun setUserWithConsentCode(userIndex: Int, consentCode: Int) {
        val parser = BluetoothBytesParser()
        parser.setUInt8(UserDataControlPointOpCode.UserConsent.value.toInt())
        parser.setUInt8(userIndex)
        parser.setUInt16(consentCode)
        val sendBytes = parser.value
        Timber.i("Writing set user with consent command bytes: ${sendBytes.asFormattedHexString()}")
        service.write(UserDataServiceHandler.UDS_CONTROL_POINT_CHARACTERISTIC_UUID, sendBytes)
    }

    fun deleteUser(userIndex: Int) {
        val parser = BluetoothBytesParser()
        parser.setUInt8(UserDataControlPointOpCode.DeleteUser.value.toInt())
        parser.setUInt8(userIndex)
        val sendBytes = parser.value
        Timber.i("Writing delete user command bytes: ${sendBytes.asFormattedHexString()}")
        service.write(UserDataServiceHandler.UDS_CONTROL_POINT_CHARACTERISTIC_UUID, sendBytes)
    }

    fun deleteAllUsers() { deleteUser(0xFF) }

    private fun newUserRegistered(value: ByteArray) {
        when(value.responseValue()) {
            RESPONSE_VALUE_SUCCESS -> {}
            RESPONSE_VALUE_INVALID_PARAMETER -> { Timber.i("ERROR: New user invalid parameter") }
            RESPONSE_VALUE_OPERATION_FAILED -> { Timber.i("ERROR: New user operation failed") }
        }
        Timber.i("New user registered returned: ${value.asFormattedHexString()}")
    }

    private fun userConsentResult(value: ByteArray) {
        logUserResult("User consent registered returned: ${value.asFormattedHexString()}")
        when(value.responseValue()) {
            RESPONSE_VALUE_SUCCESS -> logUserResult("User consent success")
            RESPONSE_VALUE_INVALID_PARAMETER -> logUserResult("ERROR: User consent invalid parameter")
            RESPONSE_VALUE_OPERATION_FAILED -> logUserResult("ERROR: User consent operation failed")
        }
    }

    private fun deleteUserResult(value: ByteArray) {
        logUserResult("Delete user returned: ${value.asFormattedHexString()}")
        when(value.responseValue()) {
            RESPONSE_VALUE_SUCCESS -> logUserResult("Delete success")
            RESPONSE_VALUE_INVALID_PARAMETER -> logUserResult("ERROR: Delete invalid parameter")
        }
    }

    private fun logUserResult(message: String) {
        UsersLog.log(message)
        Timber.i(message)
    }

    companion object {
        private const val RESPONSE_CODE_OP_CODE = 0x20.toByte()

        private const val RESPONSE_VALUE_SUCCESS = 0x01.toByte()
        private const val RESPONSE_VALUE_OP_CODE_UNSUPPORTED = 0x02.toByte()
        private const val RESPONSE_VALUE_INVALID_PARAMETER = 0x03.toByte()
        private const val RESPONSE_VALUE_OPERATION_FAILED = 0x04.toByte()
        private const val RESPONSE_VALUE_USER_UNAUTHORIZED = 0x05.toByte()
    }

    private fun ByteArray.responseRequestOpCode(): Byte = this[1]
    private fun ByteArray.responseValue(): Byte = this[1]

}