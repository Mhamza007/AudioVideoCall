package com.mhamza007.audiovideocall.model

data class User(
    var userId: String,
    var userName: String,
    var displayName: String,
    var email: String,
    var photoUrl: String
) {
    constructor() : this("", "", "", "", "")
}