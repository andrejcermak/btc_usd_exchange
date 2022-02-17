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
            executeOrder(filteredOrders.filter { it.type == OrderType.SELL }, user, order.quantity, order.type, Currency.USD,true)
        else
            executeOrder(filteredOrders.filter { it.type == OrderType.BUY }, user, order.quantity, order.type, Currency.BITCOIN, true)

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
            executeOrder(filteredOrders.filter { it.type == OrderType.SELL }, user, quantity.toLong(), OrderType.BUY, Currency.USD,  false)
        }else{
            executeOrder(filteredOrders.filter { it.type == OrderType.BUY }, user, (quantity*100000000).toLong(), OrderType.SELL, Currency.BITCOIN, false)
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

    private fun executeOrder(filteredOrderBook: List<StandingOrder>, user: User, quantity: Long, type: OrderType, spendingCurrency: Currency, isMarket: Boolean): Pair<Long, Long> {
        user.changeBalance(spendingCurrency, -quantity)
        var spent = 0L
        var bought = 0L
        for (o in filteredOrderBook){
            if(spent + o.quantity <= quantity){
                if (type == OrderType.SELL) {
                    spent += 100000000*o.quantity/o.limit
                    o.user.changeBalance(spendingCurrency, 100000000*o.quantity/o.limit)
                    bought += o.quantity
                } else {
                    spent += o.quantity
                    o.user.changeBalance(spendingCurrency, o.quantity)
                    bought += 100000000/(o.limit/o.quantity)
                }
                userSource.save(o.user)
                o.fill(o.quantity)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
            }else{
                if (type == OrderType.SELL){
                    bought += (quantity-spent)
                    o.user.changeBalance(spendingCurrency, (quantity - spent))

                } else{
                    bought += 100000000/(o.limit/(quantity-spent))
                    o.user.changeBalance(spendingCurrency, (quantity - spent))

                }
                userSource.save(o.user)
                o.fill(quantity - spent)
                if (o.quantity == 0L) orderSource.delete(o) else orderSource.save(o)
                spent = quantity
                break
            }
        }
        if(isMarket) user.changeBalance(spendingCurrency, quantity-spent)

        println("bought ${spendingCurrency.pair()}: $bought spent ${spendingCurrency}: $spent")
        user.changeBalance(spendingCurrency.pair(), bought)
        userSource.save(user)
        return Pair(bought, spent)
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