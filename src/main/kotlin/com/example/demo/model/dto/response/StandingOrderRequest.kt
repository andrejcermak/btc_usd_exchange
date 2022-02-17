package com.example.demo.model.dto.response

import com.example.demo.model.OrderType

class StandingOrderRequest (var quantity: Double, var type: OrderType, var limit: Long) {
}