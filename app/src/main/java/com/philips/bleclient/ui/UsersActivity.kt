package com.philips.bleclient.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager

class UsersActivity : AppCompatActivity() {

    private val ghsServiceHandler get() = ServiceHandlerManager.instance?.getGhsServiceHandler()
    private val usersLogView get() = findViewById<TextView>(R.id.usersLog)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_users)
            it.setDisplayHomeAsUpEnabled(true)
        }
        //ObservationSyncer.addListener(this)
        // Make the observation log scrollable
        usersLogView.setMovementMethod(ScrollingMovementMethod())
    }

    @Suppress("UNUSED_PARAMETER")
    fun newUser(view: View) {
//        UsersLog.log("Create new user")
    }

    @Suppress("UNUSED_PARAMETER")
    fun setUser(view: View) {
//        UsersLog.log("Set current user")
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteUser(view: View) {
//        UsersLog.log("Delete user")
    }

    private fun updateLogView() {
        usersLogView.setText(RacpLog.log)
        usersLogView.scrollToBottom()
    }

}