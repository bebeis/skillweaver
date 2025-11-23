package com.bebeis.skillweaver.core.domain.member

import org.mindrot.jbcrypt.BCrypt

object PasswordEncoder {
    fun encode(rawPassword: String): String {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt())
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return BCrypt.checkpw(rawPassword, encodedPassword)
    }
}
