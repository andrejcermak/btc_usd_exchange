package com.example.demo.model

import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity(name = "ExchangeUser")
class User(
    var name: String = "defaultName",
    @Id @GeneratedValue var id: Long = -1,
    var satoshis: Long = 0,
    var usdBalance: Long= 0) {

    val token: String = UUID.randomUUID().toString()

    fun changeBalance(currency: Currency, amount: Long){
        println("changing $currency balance of user $name by value $amount")
        if (getBalance(currency) + amount < 0){
            throw IllegalArgumentException("Can't spend more than ${getBalance(currency)} $currency, expected $amount")
        }
        setBalance(currency,getBalance(currency)+amount)
    }

    fun getBalances(): Map<Currency, Long> = mapOf(
        Currency.BITCOIN to satoshis,
        Currency.USD to usdBalance
    )

    fun getBalance(currency: Currency): Long = if (currency == Currency.BITCOIN) satoshis else usdBalance

    private fun setBalance(currency: Currency, amount: Long){
        if (currency == Currency.BITCOIN){
            satoshis = amount
        }else{
            usdBalance = amount
        }
    }
}