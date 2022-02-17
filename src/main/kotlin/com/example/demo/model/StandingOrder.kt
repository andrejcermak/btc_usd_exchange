package com.example.demo.model

import javax.persistence.*

@Entity
@Table(name = "StandingOrder")
data class StandingOrder(
    var quantity: Long = 0,
    var type: OrderType = OrderType.BUY,
    @Column(name = "limitValue") var limit: Long = 0,
    @ManyToOne @JoinColumn(name = "userId", nullable = false, referencedColumnName = "id") var user: User = User(),
    @GeneratedValue @Id val id: Long = -1
) {
    fun fill(volume: Long){
        quantity -= volume
    }
}