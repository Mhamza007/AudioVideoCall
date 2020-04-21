package com.mhamza007.audiovideocall.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mhamza007.audiovideocall.R
import com.mhamza007.audiovideocall.SharedPref
import com.mhamza007.audiovideocall.model.User
import com.mhamza007.audiovideocall.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseFireStore: FirebaseFirestore
    private lateinit var sharedPref: SharedPref

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = SharedPref(this)
        auth = FirebaseAuth.getInstance()
        firebaseFireStore = FirebaseFirestore.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        google_sign_in.setOnClickListener {
            if (Utils.isConnected(this)) {
                signIn()
            } else {
                Toast.makeText(this, "No Network", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, UsersActivity::class.java))
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(
            signInIntent,
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val user = User(
                    "${account.id}",
                    splitEmail("${account.email}"),
                    "${account.displayName}",
                    "${account.email}",
                    "${account.photoUrl}"
                )
                if (account.id != null || account.id != "") {
                    firebaseFireStore.collection("Users").document("${account.id}")
                        .set(user)
                        .addOnSuccessListener {
                            sharedPref.setUserId("${account.id}")
                            startActivity(Intent(this, UsersActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error Creating Profile", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun splitEmail(email: String): String {
        val emailArray = email.split("@").toTypedArray()
        return emailArray[0]
    }

    companion object {
        const val TAG = "GOOGLE";
        const val RC_SIGN_IN = 121;
    }
}
