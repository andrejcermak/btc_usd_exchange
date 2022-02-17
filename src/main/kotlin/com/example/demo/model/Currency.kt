package com.example.demo.model


enum class Currency{
    BITCOIN,
    USD;
    fun pair() =
        when(this){
            BITCOIN -> USD
            USD -> BITCOIN
        }
}