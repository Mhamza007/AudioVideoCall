package com.mhamza007.audiovideocall

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.mhamza007.audiovideocall.SinchService.SinchServiceInterface

abstract class BaseActivity : AppCompatActivity(), ServiceConnection {

    companion object {
        var sinchServiceInterface: SinchServiceInterface? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext.bindService(
            Intent(this, SinchService::class.java), this, Context.BIND_AUTO_CREATE
        )
    }

    override fun onServiceConnected(
        componentName: ComponentName,
        iBinder: IBinder
    ) {
        if (SinchService::class.java.name == componentName.className) {
            sinchServiceInterface = iBinder as SinchServiceInterface
            onServiceConnected()
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        if (SinchService::class.java.name == componentName.className) {
            sinchServiceInterface = null
            onServiceDisconnected()
        }
    }

    fun onServiceConnected() {}
    fun onServiceDisconnected() {
        // for subclasses
    }

}