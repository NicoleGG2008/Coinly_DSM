package com.example.coinly

object PasswordPolicy {
    fun valid(value: String): Boolean = value.length in 8..128 &&
        value.any { it in 'A'..'Z' } && value.any { it in 'a'..'z' } && value.any { it in '0'..'9' }
}
