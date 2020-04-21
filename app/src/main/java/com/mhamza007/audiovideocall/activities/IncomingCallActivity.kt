package com.mhamza007.audiovideocall.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
import com.sinch.android.rtc.video.VideoCallListener

class IncomingCallActivity : BaseActivity() {

    private lateinit var mCallId: String
    private lateinit var mAudioPlayer: AudioPlayer

    private lateinit var remoteUser: TextView
    private lateinit var callType: TextView
    private lateinit var callUserImage: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: CollectionReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        val answer = findViewById<Button>(R.id.answerButton)
        callUserImage = findViewById(R.id.call_user_image)
        answer.setOnClickListener(mClickListener)
        val decline = findViewById<Button>(R.id.declineButton)
        decline.setOnClickListener(mClickListener)

        mAudioPlayer = AudioPlayer(this)
        mAudioPlayer.playRingtone()
        mCallId = intent.getStringExtra(SinchService.CALL_ID)

        remoteUser = findViewById(R.id.remoteUser)
        callType = findViewById(R.id.call_type)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance().collection("Users")
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        super.onServiceConnected(componentName, iBinder)

        val call = sinchServiceInterface?.getCall(mCallId)
        if (call!!.details.isVideoOffered) {
            callType.text = "Incoming Video Call"
            call.addCallListener(SinchVideoCallListener())
        } else {
            callType.text = "Incoming Voice Call"
            call.addCallListener(SinchCallListener())
        }

        db.document(call.remoteUserId).get()
            .addOnCompleteListener {
                val name = "${it.result?.get("displayName")}"
                remoteUser.text = name
                val image = "${it.result?.get("photoUrl")}"
                Glide.with(this).load(image).into(callUserImage)
            }.addOnFailureListener {
                Log.e("SinchCall", "Database Failure Error")
            }
    }

    private val mClickListener =
        View.OnClickListener { v: View ->
            when (v.id) {
                R.id.answerButton -> answerClicked()
                R.id.declineButton -> declineClicked()
            }
        }

    private fun answerClicked() {
        mAudioPlayer.stopRingtone()
        val call = sinchServiceInterface?.getCall(mCallId)
        if (call != null) {
            call.answer()
            val intent = Intent(this, CallScreenActivity::class.java)
            intent.putExtra(SinchService.CALL_ID, mCallId)
            startActivity(intent)
        } else {
            finish()
        }
    }

    private fun declineClicked() {
        mAudioPlayer.stopRingtone()
        val call = sinchServiceInterface?.getCall(mCallId)
        call?.hangup()
        finish()
    }

    private inner class SinchCallListener : CallListener {
        override fun onCallEstablished(p0: Call?) {
            Log.d("SinchCall", "Call established")
        }

        override fun onCallProgressing(p0: Call?) {
            Log.d("SinchCall", "Call progressing")
        }

        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {}

        override fun onCallEnded(p0: Call?) {
            Log.d("SinchCall", "Call ended, cause: ${p0?.details?.endCause}")
            Log.d("SinchCall", "Call ended, duration: ${p0?.details?.duration}")
            mAudioPlayer.stopRingtone()
            finish()
        }
    }

    private inner class SinchVideoCallListener : VideoCallListener {
        override fun onVideoTrackAdded(p0: Call?) {}

        override fun onVideoTrackPaused(p0: Call?) {}

        override fun onCallEstablished(p0: Call?) {
            Log.d("SinchCall", "Call established")
        }

        override fun onVideoTrackResumed(p0: Call?) {}

        override fun onCallProgressing(p0: Call?) {
            Log.d("SinchCall", "Call progressing")
        }

        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {}

        override fun onCallEnded(p0: Call?) {
            Log.d("SinchCall", "Call ended, cause: ${p0?.details?.endCause}")
            Log.d("SinchCall", "Call ended, duration: ${p0?.details?.duration}")
            mAudioPlayer.stopRingtone()
            finish()
        }
    }
}
