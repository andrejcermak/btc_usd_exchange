package com.example.demo.datasource

import com.example.demo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserDataSource: JpaRepository<User, Long> {
    @Query("select id from ExchangeUser where token = ?1")
    fun getUserFromToken(token: String): Long?
}