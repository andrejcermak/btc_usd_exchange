package com.example.demo.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "User Not Found")
class AuthException : Exception() {
}