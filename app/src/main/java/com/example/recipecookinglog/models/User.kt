package com.example.recipecookinglog.models

data class User(
    var uid: String = "",
    var email: String = "",
    var displayName: String = ""
) {
    constructor() : this("", "", "")
}