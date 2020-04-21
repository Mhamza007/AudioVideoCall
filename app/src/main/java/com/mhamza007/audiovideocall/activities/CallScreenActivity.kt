package com.mhamza007.audiovideocall.activities

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mhamza007.audiovideocall.AudioPlayer
import com.mhamza007.audiovideocall.BaseActivity
import com.mhamza007.audiovideocall.R
import com.mhamza007.audiovideocall.SinchService
import com.sinch.android.rtc.PushPair
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.CallState
import com.sinch.android.rtc.video.VideoCallListener
import java.util.*

class CallScreenActivity : BaseActivity() {

    var CALL_START_TIME = "callStartTime"
    var ADDED_LISTENER = "addedListener"

    private lateinit var mAudioPlayer: AudioPlayer
    private lateinit var mTimer: Timer
    private lateinit var mDurationTask: UpdateCallDurationTask

    private lateinit var mCallId: String
    var mCallStart = 0L
    var mAddedListener = false
    var mVideoViewsAdded = false

    private lateinit var mCallDuration: TextView
    private lateinit var mCallState: TextView
    private lateinit var mCallerName: TextView
    private lateinit var callUserImage: ImageView

    var call: Call? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: CollectionReference

    inner class UpdateCallDurationTask : TimerTask() {
        override fun run() {
            runOnUiThread(this@CallScreenActivity::updateCallDuration)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putLong(CALL_START_TIME, mCallStart)
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mCallStart = savedInstanceState.getLong(CALL_START_TIME)
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_screen)

        mAudioPlayer = AudioPlayer(this)
        mCallDuration = findViewById(R.id.callDuration)
        mCallerName = findViewById(R.id.remoteUser)
        mCallState = findViewById(R.id.callState)
        callUserImage = findViewById(R.id.call_user_image)
        val endCallButton = findViewById<Button>(R.id.hangupButton)

        endCallButton.setOnClickListener {
            endCall()
        }
        mCallStart = System.currentTimeMillis()
        mCallId = intent.getStringExtra(SinchService.CALL_ID)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance().collection("Users")
    }


    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        super.onServiceConnected(componentName, iBinder)

        call = sinchServiceInterface?.getCall(mCallId)
        if (call != null) {
            if (call!!.details.isVideoOffered) {
                if (!mAddedListener) {
                    call?.addCallListener(SinchVideoCallListener())
                    mAddedListener = true
                }
            } else {
                call?.addCallListener(SinchCallListener())
                mCallState.text = call?.state.toString()
            }

            db.document(call!!.remoteUserId).get()
                .addOnCompleteListener {
                    val name = "${it.result?.get("displayName")}"
                    mCallerName.text = name
                    val image = "${it.result?.get("photoUrl")}"
                    Glide.with(this).load(image).into(callUserImage)
                }
                .addOnFailureListener {
                    Log.e("SinchCall", "Database Failure Error")
                }

            if (call?.state == CallState.ESTABLISHED)
                addVideoViews()
        } else {
            Log.e("SinchCall", "Started with invalid callId, aborting.")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        if (call!!.details.isVideoOffered) {
            mDurationTask.cancel()
            mTimer.cancel()

            removeVideoViews()
            endCall()
        }
    }

    override fun onResume() {
        super.onResume()

        mTimer = Timer()
        mDurationTask = UpdateCallDurationTask()
        mTimer.schedule(mDurationTask, 0, 500)
    }

    override fun onBackPressed() {}

    private fun endCall() {
        mAudioPlayer.stopProgressTone()
        val call = sinchServiceInterface?.getCall(mCallId)
        call?.hangup()
        finish()
    }

    private fun formatTimeSpan(timeSpan: Long): String {
        val totalSeconds: Long = timeSpan / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun updateCallDuration() {
        if (mCallStart > 0)
            mCallDuration.text = formatTimeSpan(System.currentTimeMillis() - mCallStart)
    }

    private fun addVideoViews() {
        if (mVideoViewsAdded || sinchServiceInterface == null) {
            return //early
        }
        val vc = sinchServiceInterface?.videoController
        if (vc != null) {
            val localView = findViewById<RelativeLayout>(R.id.localVideo)
            localView.addView(vc.localView)

            localView.setOnClickListener {
                //this toggles the front camera to rear camera and vice versa
                vc.toggleCaptureDevicePosition()
            }

            val view = findViewById<LinearLayout>(R.id.remoteVideo)
            view.addView(vc.remoteView)
            mVideoViewsAdded = true
        }
    }

    private fun removeVideoViews() {
        if (sinchServiceInterface == null) {
            return // early
        }
        val vc = sinchServiceInterface?.videoController
        if (vc != null) {
            val view = findViewById<LinearLayout>(R.id.remoteVideo)
            view.removeView(vc.remoteView)

            val localView = findViewById<RelativeLayout>(R.id.localVideo)
            localView.removeView(vc.localView)
            mVideoViewsAdded = false
        }
    }

    private inner class SinchCallListener : CallListener {
        override fun onCallEstablished(p0: Call?) {
            Log.d("SinchCall", "Call established")
            mAudioPlayer.stopProgressTone()
            mCallState.text = p0?.state.toString()
            volumeControlStream = AudioManager.STREAM_VOICE_CALL
            mCallStart = System.currentTimeMillis()
        }

        override fun onCallProgressing(p0: Call?) {
            Log.d("SinchCall", "Call progressing")
            mAudioPlayer.playProgressTone()
        }

        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {}

        override fun onCallEnded(p0: Call?) {
            Log.d("SinchCall", "Call ended, cause: ${p0?.details?.endCause}")
            Log.d("SinchCall", "Call ended, duration: ${p0?.details?.duration}")
            mAudioPlayer.stopRingtone()
            volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
            Log.d("SinchCall", "endMsg: ${p0?.details}")
            Toast.makeText(this@CallScreenActivity, "Call ended", Toast.LENGTH_LONG).show()
            endCall()
        }
    }

    private inner class SinchVideoCallListener : VideoCallListener {
        override fun onVideoTrackAdded(p0: Call?) {
            Log.d("SinchCall", "Video track added")
            addVideoViews()
        }

        override fun onVideoTrackPaused(p0: Call?) {}

        override fun onCallEstablished(p0: Call?) {
            Log.d("SinchCall", "Call established")
            mAudioPlayer.stopProgressTone()
            mCallState.text = call?.state.toString()
            volumeControlStream = AudioManager.STREAM_VOICE_CALL
            val audioController = sinchServiceInterface?.audioController
            audioController?.enableSpeaker()
            mCallStart = System.currentTimeMillis()
            Log.d("SinchCall", "Call offered video: " + call!!.details.isVideoOffered)
        }

        override fun onVideoTrackResumed(p0: Call?) {}

        override fun onCallProgressing(p0: Call?) {
            Log.d("SinchCall", "Call progressing")
            mAudioPlayer.playProgressTone()
        }

        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {}

        override fun onCallEnded(p0: Call?) {
            Log.d("SinchCall", "Call ended, cause: ${p0?.details?.endCause}")
            Log.d("SinchCall", "Call ended, duration: ${p0?.details?.duration}")
            mAudioPlayer.stopRingtone()
            Log.d("SinchCall", "Call End Msg: ${p0?.details}")
            Toast.makeText(this@CallScreenActivity, "Call ended", Toast.LENGTH_LONG).show()
            endCall()
        }
    }
}
