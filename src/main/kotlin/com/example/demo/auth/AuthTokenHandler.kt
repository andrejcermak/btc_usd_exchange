package com.example.demo.auth

import com.example.demo.datasource.UserDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AuthTokenHandler {
    @Autowired
    lateinit var userDataSource: UserDataSource

    @Transactional(readOnly = true)
    fun getUserFromToken(token : String?) : Long? {
        if (token == null)
            throw IllegalArgumentException()
        return userDataSource.getUserFromToken(token)
    }
}