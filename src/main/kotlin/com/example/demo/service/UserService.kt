package com.example.demo.service

import com.example.demo.datasource.UserDataSource
import com.example.demo.model.Currency
import com.example.demo.model.User
import com.example.demo.model.dto.requests.TopUpRequest
import org.springframework.stereotype.Service

@Service
class UserService(private val dataSource: UserDataSource) {
    fun registerUser(user: User): String {
        dataSource.saveAndFlush(user)
        return user.token
    }
    fun getUsersBalance(userId: Long): Map<Currency, Long> = dataSource.findById(userId).orElseThrow {IllegalArgumentException("User not found") }.getBalances()

    fun fundUsersBalance(userId: Long, fund: TopUpRequest): Any {
        val user = dataSource.findById(userId).orElseThrow {IllegalArgumentException("User not found") }
        if(fund.currency == Currency.BITCOIN)
            user.changeBalance(fund.currency, (fund.amount*100000000).toLong())
        else
            user.changeBalance(fund.currency, (fund.amount).toLong())
        return dataSource.save(user)
    }
    fun getUsers(): List<User> = dataSource.findAll()
}
