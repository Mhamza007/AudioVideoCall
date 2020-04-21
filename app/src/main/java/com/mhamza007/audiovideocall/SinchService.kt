package com.mhamza007.audiovideocall

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.mhamza007.audiovideocall.activities.IncomingCallActivity
import com.sinch.android.rtc.*
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallClientListener
import com.sinch.android.rtc.video.VideoController

class SinchService : Service() {
    private val mSinchServiceInterface = SinchServiceInterface()
    var mSinchClient: SinchClient? = null
    private var userName: String? = null
    private var mListener: StartFailedListener? = null

    override fun onDestroy() {
        if (mSinchClient != null && mSinchClient!!.isStarted) {
            mSinchClient!!.terminate()
        }
        super.onDestroy()
    }

    private fun start(userName: String) {
        if (mSinchClient == null) {
            this.userName = userName
            mSinchClient = Sinch.getSinchClientBuilder()
                .context(applicationContext)
                .userId(userName)
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT)
                .build()
            mSinchClient?.setSupportCalling(true)
            mSinchClient?.startListeningOnActiveConnection()
            mSinchClient?.addSinchClientListener(MySinchClientListener())
            mSinchClient?.callClient?.addCallClientListener(SinchCallClientListener())
            mSinchClient?.start()
        }
    }

    private fun stop() {
        if (mSinchClient != null) {
            mSinchClient!!.terminate()
            mSinchClient = null
        }
    }

    private val isStarted: Boolean
        get() = mSinchClient != null && mSinchClient!!.isStarted

    override fun onBind(intent: Intent): IBinder? {
        return mSinchServiceInterface
    }

    inner class SinchServiceInterface : Binder() {
        fun callUserVideo(userId: String?): Call {
            return mSinchClient!!.callClient.callUserVideo(userId)
        }

        fun callUser(userId: String?): Call {
            return mSinchClient!!.callClient.callUser(userId)
        }

        val isStarted: Boolean
            get() = this@SinchService.isStarted

        fun startClient(userName: String) {
            start(userName)
        }

        fun getCall(callId: String?): Call {
            return mSinchClient!!.callClient.getCall(callId)
        }

        val videoController: VideoController?
            get() = if (!isStarted) {
                null
            } else mSinchClient!!.videoController

        val audioController: AudioController?
            get() = if (!isStarted) {
                null
            } else mSinchClient!!.audioController
    }

    interface StartFailedListener {
        fun onStartFailed(error: SinchError?)
        fun onStarted()
    }

    private inner class MySinchClientListener : SinchClientListener {
        override fun onClientFailed(client: SinchClient, error: SinchError) {
            if (mListener != null) {
                mListener!!.onStartFailed(error)
            }
            mSinchClient!!.terminate()
            mSinchClient = null
        }

        override fun onClientStarted(client: SinchClient) {
            Log.d(TAG, "SinchClient started")
            if (mListener != null) {
                mListener!!.onStarted()
            }
        }

        override fun onClientStopped(client: SinchClient) {
            Log.d(TAG, "SinchClient stopped")
        }

        override fun onLogMessage(
            level: Int,
            area: String,
            message: String
        ) {
            when (level) {
                Log.DEBUG -> Log.d(area, message)
                Log.ERROR -> Log.e(area, message)
                Log.INFO -> Log.i(area, message)
                Log.VERBOSE -> Log.v(area, message)
                Log.WARN -> Log.w(area, message)
            }
        }

        override fun onRegistrationCredentialsRequired(
            client: SinchClient,
            clientRegistration: ClientRegistration
        ) {
        }
    }

    private inner class SinchCallClientListener :
        CallClientListener {
        override fun onIncomingCall(
            callClient: CallClient,
            call: Call
        ) {
            val intent = Intent(this@SinchService, IncomingCallActivity::class.java)
            intent.putExtra(CALL_ID, call.callId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "Incoming call")
            this@SinchService.startActivity(intent)
        }
    }

    companion object {
        private const val APP_KEY = "YOUR_SINCH_APP_KEY"
        private const val APP_SECRET = "YOUR_SINCH_APP_SECRET"
        private const val ENVIRONMENT = "clientapi.sinch.com"
        const val CALL_ID = "CALL_ID"
        val TAG = SinchService::class.java.simpleName
    }
}