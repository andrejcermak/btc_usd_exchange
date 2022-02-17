package com.example.demo.model.dto.requests

import com.example.demo.model.OrderType

class MarketOrderRequest(var quantity: Long, val type: OrderType) {
}