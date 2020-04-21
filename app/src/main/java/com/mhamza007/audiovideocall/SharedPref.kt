package com.mhamza007.audiovideocall

import android.content.Context

class SharedPref {
    private var context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun setUserId(userId: String) {
        val sharedPreferences =
            context.getSharedPreferences("SharedPreference", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userId", userId)
        editor.apply()
    }

    fun getUserId(): String {
        val sharedPreferences =
            context.getSharedPreferences("SharedPreference", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userId", "")!!
    }

}