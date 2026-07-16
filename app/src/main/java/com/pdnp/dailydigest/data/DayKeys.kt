package com.pdnp.dailydigest.data

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DayKeys {
    fun of(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun today(): String = LocalDate.now().toString()

    fun yesterday(): String = LocalDate.now().minusDays(1).toString()

    fun timeOf(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    /** Stable hash used to avoid storing the same message twice. */
    fun dedupe(vararg parts: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(parts.joinToString("|").toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
