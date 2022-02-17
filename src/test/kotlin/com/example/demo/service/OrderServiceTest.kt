package com.example.demo.service

import com.example.demo.datasource.OrderDataSource
import com.example.demo.datasource.UserDataSource
import com.example.demo.model.Currency
import com.example.demo.model.OrderType
import com.example.demo.model.StandingOrder
import com.example.demo.model.User
import com.example.demo.model.dto.requests.MarketOrderRequest
import com.example.demo.model.dto.response.StandingOrderRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.annotation.DirtiesContext
import java.util.*

internal class OrderServiceTest {
    private var userDataSource: UserDataSource = mockk()
    private var orderDataSource: OrderDataSource = mockk()

    private val orderService = OrderService(orderDataSource, userDataSource)
    private val userService = UserService(userDataSource)


    @Test
    fun `should fill market order` (){
        // given
        val user1 = User("1")
        every { userDataSource.findAll() } returns listOf(
            User("rich", usdBalance = 100000),
            user1)
        val users = userService.getUsers()
        every { orderDataSource.findAll() } returns listOf(
            StandingOrder(10, OrderType.SELL, 40000, user1)
        )
        val rich = users.find { it.name == "rich" }!!
        every { userDataSource.findById(rich.id) } returns Optional.of(rich)

        // when
        val response = orderService.createMarketOrder(
            MarketOrderRequest(40000, OrderType.BUY),
            rich.id)

        // then
        assert(rich.getBalance(Currency.BITCOIN) == 1L)
        assert(response.averagePrice == 40000.0)
        assert(response.quantity == 40000L)
        assert(users.find { it.name == "1" }!!
            .getBalance(Currency.USD) == 40000L)
    }

    @Test
    fun `should fill market order and close all remaining standing orders` (){
        // given
        val user1 = User("1")
        every { userDataSource.findAll() } returns listOf(
            User("rich", usdBalance = 1000000),
            user1,
            User("2"),
            User("3"))
        val users = userService.getUsers()
        every { orderDataSource.findAll() } returns listOf(
            StandingOrder(10, OrderType.SELL, 40000, user1),
            StandingOrder(2, OrderType.SELL, 42000, users[2]),
            StandingOrder(1, OrderType.SELL, 43000, users[3]))

        // when
        val response = orderService.createMarketOrder(
            MarketOrderRequest(400000+2*42000+43000, OrderType.BUY),
            users[0].id)

        // then
        val list: List<StandingOrder> = orderService.getOrders()

        assert(user1.getBalance(Currency.USD) == 400000L)
        assert(users[2].getBalance(Currency.USD) == 84000L)
        assert(users[3].getBalance(Currency.USD) == 43000L)

    }

    @Test
    fun `should fill standing order` (){
        // given
        val user1 = User("1")
        every { userDataSource.findAll() } returns listOf(
            User("rich", usdBalance = 100000),
            user1)
        val users = userService.getUsers()
        every { orderDataSource.findAll() } returns listOf(
            StandingOrder(10, OrderType.SELL, 40000, user1)
        )
        val rich = users.find { it.name == "rich" }!!
        val rich_usd = rich.getBalance(Currency.USD)

        // when
        orderService.createStandingOrder(
            StandingOrderRequest(40000.0, OrderType.BUY, 41000),
            rich.id)


        // then
        assert(rich.getBalance(Currency.USD) + 40000 == rich_usd)

    }
    @Test
    @DirtiesContext
    fun `should not allow second order` (){
        // given
        every { userDataSource.findAll() } returns listOf(
            User("poor", usdBalance = 10))
        val poor = userService.getUsers().find { it.name == "poor" }!!
        val firstOrder = StandingOrderRequest(10.0, OrderType.BUY, 20000)
        val standing = StandingOrder(firstOrder.quantity.toLong(), firstOrder.type, firstOrder.limit, poor)
        every {orderDataSource.findAll()} returns listOf()
        every {orderDataSource.save(standing)} returns standing
        // when
        orderService.createStandingOrder(
            firstOrder,
            poor.id)
        assertThrows<IllegalArgumentException> {
            orderService.createStandingOrder(
            firstOrder,
            poor.id)}
        // then

    }
    @Test
    fun `should place standing order and lock funds` (){
        // given
        val poor = User("poor", usdBalance = 10)
        every { userDataSource.findAll() } returns listOf(poor)

        val firstOrder = StandingOrderRequest(10.0, OrderType.BUY, 20000)
        val standing = StandingOrder(firstOrder.quantity.toLong(), firstOrder.type, firstOrder.limit, poor)
        every {orderDataSource.save(standing)} returns standing
        every {orderDataSource.findAll()} returns listOf()

        // when
        orderService.createStandingOrder(
            firstOrder,
            poor.id)

        // then
        assert(poor.usdBalance == 0L && poor.satoshis == 0L)
    }
}