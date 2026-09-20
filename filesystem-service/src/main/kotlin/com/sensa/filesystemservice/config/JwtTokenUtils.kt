package com.sensa.filesystemservice.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenUtils(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {

    private lateinit var secretKey: SecretKey

    @jakarta.annotation.PostConstruct
    fun init() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
    }

    fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun isValidToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = parseToken(token)
        return UUID.fromString(claims["userId"] as String)
    }

    fun getEmailFromToken(token: String): String {
        return parseToken(token).subject
    }
}
