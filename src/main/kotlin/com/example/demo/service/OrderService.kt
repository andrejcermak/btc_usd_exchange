package com.example.demo.service

import com.example.demo.datasource.OrderDataSource
import com.example.demo.datasource.UserDataSource
import com.example.demo.model.Currency
import com.example.demo.model.OrderType
import com.example.demo.model.StandingOrder
import com.example.demo.model.User
import com.example.demo.model.dto.requests.MarketOrderRequest
import com.example.demo.model.dto.response.MarketOrderResponse
import com.example.demo.model.dto.response.StandingOrderRequest
import org.springframework.stereotype.Service

@Service
class OrderService(private val orderSource: OrderDataSource, private val userSource: UserDataSource) {
    fun getOrders(): List<StandingOrder> {
        return orderSource.findAll()
    }

    fun createMarketOrder(order: MarketOrderRequest, userId: Long): MarketOrderResponse {
        val user = userSource.findById(userId).get()
        val filteredOrders = orderSource.findAll().filter { it.type != order.type }.sortedBy { it.limit }
        val (bought,  spent) = if (order.type == OrderType.BUY)
            executeBuyOrder(filteredOrders.filter { it.type == OrderType.SELL }, user, order.quantity, true)
        else
            executeSellOrder(filteredOrders.filter { it.type == OrderType.SELL }, user, order.quantity, true)

        orderSource.flush()
        userSource.flush()
        return MarketOrderResponse(spent, spent/bought.toDouble())

    }

    fun createStandingOrder(newOrder: StandingOrderRequest, userId: Long): String {
        val user = userSource.findById(userId).get()
        val filteredOrders = orderSource.findAll()
            .filter { if(newOrder.type == OrderType.BUY) it.limit<=newOrder.limit else it.limit<=newOrder.limit }
            .sortedBy { it.limit }

        val quantity =  newOrder.quantity
        val p: Pair<Long, Long> = if(newOrder.type == OrderType.BUY) {
            executeBuyOrder(filteredOrders.filter { it.type == OrderType.SELL }, user, quantity.toLong(), false)
        }else{
            executeSellOrder(filteredOrders.filter { it.type == OrderType.BUY }, user, (quantity*100000000).toLong(), false)
        }

        var standingOrder = StandingOrder(quantity.toLong(), newOrder.type, newOrder.limit, user)
        standingOrder.fill(p.second)

        if(standingOrder.quantity > 0){
            standingOrder = orderSource.save(standingOrder)
        }
        orderSource.flush()
        userSource.flush()
        return standingOrder.id.toString()

    }

    private fun executeSellOrder(filteredOrderBook: List<StandingOrder>, user: User, quantity: Long, isMarket: Boolean): Pair<Long, Long> {
        val spendingCurrency = Currency.BITCOIN
        user.changeBalance(spendingCurrency, -quantity)
        println("SELL ORDER")
        var spentBitcoin = 0L
        var boughtUSD = 0L
        for (o in filteredOrderBook){
            if(spentBitcoin + o.quantity <= quantity){
                spentBitcoin += 100000000*o.quantity/o.limit
                println("$spentBitcoin")
                o.user.changeBalance(spendingCurrency, 100000000*o.quantity/o.limit)
                userSource.save(o.user)
                boughtUSD += o.quantity
                o.fill(o.quantity)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
            }else{
                println("$quantity, $spentBitcoin, ${o.limit}")
                boughtUSD += (quantity-spentBitcoin)
                o.user.changeBalance(spendingCurrency, (quantity - spentBitcoin))
                userSource.save(o.user)
                o.fill(quantity - spentBitcoin)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
                spentBitcoin = quantity
                break
            }
        }
        if(isMarket){
            user.changeBalance(spendingCurrency, quantity-spentBitcoin)
        }
        println("bought ${spendingCurrency.pair()}: $boughtUSD spent ${spendingCurrency}: $spentBitcoin")
        user.changeBalance(spendingCurrency.pair(), boughtUSD)
        userSource.save(user)
        return Pair(boughtUSD, spentBitcoin)
    }

    private fun executeBuyOrder(filteredOrderBook: List<StandingOrder>, user: User, quantity: Long, isMarket: Boolean): Pair<Long, Long>{
        val spendingCurrency = Currency.USD
        user.changeBalance(spendingCurrency, -quantity)
        println("BUY ORDER")
        var spentUsd = 0L
        var boughtBitcoin = 0L
        for (o in filteredOrderBook){
            println(o.id)
            if(spentUsd + o.quantity <= quantity){
                spentUsd += o.quantity
                o.user.changeBalance(spendingCurrency, o.quantity)
                userSource.save(o.user)
                boughtBitcoin += 100000000/(o.limit/o.quantity)
                o.fill(o.quantity)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
            }else{
                println("${o.limit} $quantity, $spentUsd")
                boughtBitcoin += 100000000/(o.limit/(quantity-spentUsd))
                o.user.changeBalance(spendingCurrency, (quantity - spentUsd))
                userSource.save(o.user)
                o.fill(quantity - spentUsd)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
                spentUsd = quantity
                break
            }
        }
        if(isMarket){
            user.changeBalance(spendingCurrency, quantity-spentUsd)
        }
        println("bought ${spendingCurrency.pair()}: $boughtBitcoin spent ${spendingCurrency}: $spentUsd")
        user.changeBalance(spendingCurrency.pair(), boughtBitcoin)
        userSource.save(user)
        return Pair(boughtBitcoin, spentUsd)
    }

    fun deleteStandingOrder(orderId: Long, userId: Long) {
        val order = orderSource.findById(orderId).get()
        val user = userSource.findById(userId).get()
        if(userId == order.user.id) {
            orderSource.delete(order)
            if (order.type == OrderType.BUY) {
                user.changeBalance(Currency.USD, order.quantity)
            } else {
                user.changeBalance(Currency.BITCOIN, order.quantity)
            }
            userSource.save(user)
            userSource.flush()
            orderSource.flush()
        } else{
            throw IllegalArgumentException("forbidden operation")
        }
    }

}