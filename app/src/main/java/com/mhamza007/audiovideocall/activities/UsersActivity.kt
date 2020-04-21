package com.mhamza007.audiovideocall.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mhamza007.audiovideocall.BaseActivity
import com.mhamza007.audiovideocall.R
import com.mhamza007.audiovideocall.SharedPref
import com.mhamza007.audiovideocall.SinchService
import com.mhamza007.audiovideocall.model.User
import com.mhamza007.audiovideocall.utils.Utils
import com.sinch.android.rtc.calling.Call
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.user_item_layout.view.*
import kotlin.properties.Delegates

class UsersActivity : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPref
    private lateinit var currentUserId: String

    private var call: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        //Init Firestore
        db = FirebaseFirestore.getInstance()

        sharedPref = SharedPref(this)
        currentUserId = sharedPref.getUserId()

        try {
            if (!sinchServiceInterface?.isStarted!!) {
                sinchServiceInterface?.startClient(currentUserId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        screenWidth = Utils.getScreenWidth(this)

        fetchUsers()
    }

    private fun fetchUsers(): User {
        var user = User()
        val adapter = GroupAdapter<GroupieViewHolder>()
        db.collection("Users").get()
            .addOnCompleteListener {
                for (doc in it.result!!) {
                    user = User(
                        doc.getString("userId")!!,
                        doc.getString("userName")!!,
                        doc.getString("displayName")!!,
                        doc.getString("email")!!,
                        doc.getString("photoUrl")!!
                    )
                    adapter.add(UserItem(this, user))

                    usersRecyclerView.layoutManager = GridLayoutManager(this, 2)
                    usersRecyclerView.adapter = adapter
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                Log.e("UsersActivity", "Error : ${it.message}")
            }
        return user
    }

    companion object {
        var screenWidth by Delegates.notNull<Int>()
    }

    inner class UserItem(var context: Context, var user: User) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int {
            return R.layout.user_item_layout
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.layoutParams.width = screenWidth / 2
            viewHolder.itemView.layoutParams.height = screenWidth / 2

            Glide.with(context).load(Uri.parse(user.photoUrl)).into(viewHolder.itemView.userImage)

            viewHolder.itemView.displayName.text = user.displayName

            viewHolder.itemView.voiceCall.setOnClickListener {
                // Voice call
                Dexter.withActivity(this@UsersActivity)
                    .withPermissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                if (!sinchServiceInterface?.isStarted!!) {
                                    sinchServiceInterface?.startClient(currentUserId)
                                } else {
                                    call = sinchServiceInterface?.callUser(user.userId)
                                    val callId = call?.callId

                                    val intent =
                                        Intent(this@UsersActivity, CallScreenActivity::class.java)
                                    intent.putExtra(SinchService.CALL_ID, callId)
                                    startActivity(intent)
                                }
                            } else {
                                Toast.makeText(
                                    this@UsersActivity,
                                    "Permissions Denied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }
                    })
                    .onSameThread()
                    .check()
            }

            viewHolder.itemView.videoCall.setOnClickListener {
                // Video call
                Dexter.withActivity(this@UsersActivity)
                    .withPermissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CAMERA
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                if (!sinchServiceInterface?.isStarted!!) {
                                    sinchServiceInterface?.startClient(currentUserId)
                                } else {
                                    call = sinchServiceInterface?.callUserVideo(user.userId)
                                    val callId = call?.callId

                                    val intent =
                                        Intent(this@UsersActivity, CallScreenActivity::class.java)
                                    intent.putExtra(SinchService.CALL_ID, callId)
                                    startActivity(intent)
                                }
                            } else {
                                Toast.makeText(
                                    this@UsersActivity,
                                    "Permissions Denied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }
                    })
                    .onSameThread()
                    .check()
            }
        }
    }
}