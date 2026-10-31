package com.juhao.rollcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.juhao.rollcall.state.RollCallController

class MainActivity : ComponentActivity() {

    private var controller: RollCallController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rollCallController = RollCallController(applicationContext)
        controller = rollCallController
        setContent {
            RollCallApp(rollCallController)
        }
    }

    override fun onDestroy() {
        controller?.dispose()
        controller = null
        super.onDestroy()
    }
}
