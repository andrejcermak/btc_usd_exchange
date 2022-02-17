package com.example.demo.datasource

import com.example.demo.model.StandingOrder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderDataSource: JpaRepository<StandingOrder, Long> {
}