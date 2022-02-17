package com.example.demo.controller

import com.example.demo.auth.Auth
import com.example.demo.model.User
import com.example.demo.model.dto.requests.TopUpRequest
import com.example.demo.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val service: UserService){

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registerUser(@RequestBody user: User): String = service.registerUser(user)

    @GetMapping("/balance")
    fun getUsersBalance(@Auth userId: Long) = service.getUsersBalance(userId)

    @PostMapping("/balance")
    @ResponseStatus(HttpStatus.CREATED)
    fun fundUsersBalance(@Auth userId: Long, @RequestBody fund: TopUpRequest) = service.fundUsersBalance(userId, fund)
}