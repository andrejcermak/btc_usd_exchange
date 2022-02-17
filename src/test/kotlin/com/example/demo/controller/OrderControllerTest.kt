package com.example.demo.controller

import com.example.demo.datasource.OrderDataSource
import com.example.demo.datasource.UserDataSource
import com.example.demo.model.Currency
import com.example.demo.model.OrderType
import com.example.demo.model.StandingOrder
import com.example.demo.model.User
import com.example.demo.model.dto.requests.MarketOrderRequest
import com.example.demo.model.dto.requests.TopUpRequest
import com.example.demo.model.dto.response.StandingOrderRequest
import com.example.demo.service.OrderService
import com.example.demo.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
)
{
    private var userDataSource: UserDataSource = mockk()
    private var orderDataSource: OrderDataSource = mockk()

    private val orderService = OrderService(orderDataSource, userDataSource)
    private val userService = UserService(userDataSource)

    @Nested
    @DisplayName("POST /api/orders/market")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PostMarketOrder {
        @Test
        fun `should create a market order that fills completely` (){
            // given
            val marketOrder: MarketOrderRequest = MarketOrderRequest(10, OrderType.BUY)
            val user1 = User("1")
            val rich = User("rich", usdBalance = 1000000)
            every { userDataSource.getUserFromToken(rich.token) } returns rich.id
            every { userDataSource.findAll() } returns listOf(
                rich,
                user1,
                User("2"),
                User("3"))
            val users = userService.getUsers()
            every { orderDataSource.findAll() } returns listOf(
                StandingOrder(10, OrderType.SELL, 40000, user1),
                StandingOrder(2, OrderType.SELL, 42000, users[2]),
                StandingOrder(1, OrderType.SELL, 43000, users[3]))

            // when
            val performPost = mockMvc.post("/api/orders/market"){
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(marketOrder)
                header("authToken", rich.token)
            }
            // then
            performPost
                .andDo { print() }
                .andExpect {
                    status { isCreated() }
                    content {
                        jsonPath("$.quantity") { value(marketOrder.quantity)}
                        jsonPath("$.averagePrice") { value(40000) }
                    }
                }
        }
        
        @Test
        fun `should  create a partially filled market order` (){
            // given
            val marketOrder = MarketOrderRequest(600000, OrderType.BUY)

            // when
            val performPost = mockMvc.post("/api/orders/market"){
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(marketOrder)
                header("authToken", "rich")
            }

            // then
            performPost
                .andDo { print() }
                .andExpect {
                    status { isCreated()
                    content {
                        jsonPath("$.quantity") {value( 527000)}
                    }}
                }
        }
        
        @Test
        fun `should fail because of missing funds` (){

            val marketOrder: MarketOrderRequest = MarketOrderRequest(100, OrderType.BUY)

            // when
            val performPost = mockMvc.post("/api/orders/market"){
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(marketOrder)
                header("authToken", "default")
            }

            // then
            performPost
                .andDo { print() }
                .andExpect {
                    status { isForbidden() }
                    }
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RegisterUser {
        @Test
        fun `should create a new user` (){
            // given
            val newUser = User("Andrej")


            // when
            val performPost = mockMvc.post("/api/users"){
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(newUser)
            }

            // then
            performPost
                .andDo { print() }
                .andExpect {
                    status { isCreated() }
                }

        }
    }
    
    @Nested
    @DisplayName("GET /api/users/balance")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetBalance {
        @Test
        fun `should get users balance` (){
            val response = mockMvc.get("/api/users/balance"){
                header("authToken", "4305cf6c-74ce-4e86-9b88-815a5a887457") }
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                }.andReturn()
            println(response)
        }
    }

    @Nested
    @DisplayName("PUT /api/users/balance")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TopUpBalance {
        @Test
        fun `should fund the account of the user` (){
            // when/then
            val topUp = TopUpRequest(10.0, Currency.BITCOIN)
            mockMvc.post("/api/users/balance"){
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(topUp)
                header("authToken", "default")
            }
                .andDo { print() }
                .andExpect { status { isCreated() } }
        }
    }

    @Nested
    @DisplayName("GET /api/orders/standing")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetStandingOrders {
        @Test
        fun `should get standing order`() {
            // given
            mockMvc.get("/api/orders/standing")
                .andDo { print() }
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("POST /api/orders/standing")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateStandingOrder {
        @Test
        fun `should place a standing order` (){
            // given
            val newStandingOrder = StandingOrderRequest(40000.0, OrderType.BUY, 40000)

            // when
            val performPostStandingOrder = mockMvc.post("/api/orders/standing") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(newStandingOrder)
                header("authToken", "rich")
            }

            // then
            performPostStandingOrder
                .andDo { print() }
                .andExpect { status { isCreated() } }
            mockMvc.get("/api/orders/standing").andDo { print() }
        }
    }


    @Nested
    @DisplayName("DELETE /api/orders/standing")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RemoveStandingOrder {
    }

}