package com.example.demo.controller

import com.example.demo.auth.Auth
import com.example.demo.model.StandingOrder
import com.example.demo.model.dto.requests.MarketOrderRequest
import com.example.demo.model.dto.response.MarketOrderResponse
import com.example.demo.model.dto.response.StandingOrderRequest
import com.example.demo.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val service: OrderService) {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<String> =
        ResponseEntity(e.message, HttpStatus.FORBIDDEN)

    @GetMapping("/standing")
    fun getOrders(): Collection<StandingOrder> = service.getOrders()

    @PostMapping("/standing")
    @ResponseStatus(HttpStatus.CREATED)
    fun postStandingOrder(@Auth userId: Long,
                        @RequestBody order: StandingOrderRequest
    ) = service.createStandingOrder(order, userId)

    @DeleteMapping("/standing")
    @ResponseStatus(HttpStatus.CREATED)
    fun deleteStandingOrder(@Auth userId: Long, @RequestParam(name = "id") orderId: Long) = service.deleteStandingOrder(orderId, userId)

    @PostMapping("/market")
    @ResponseStatus(HttpStatus.CREATED)
    fun postMarketOrder(@Auth userId: Long,
                        @RequestBody order: MarketOrderRequest
    ): MarketOrderResponse = service.createMarketOrder(order, userId)
}

