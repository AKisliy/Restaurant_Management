package models

import infrastructure.ObservableList
import interfaces.IUser
import kotlinx.serialization.Serializable

/**
 * User - model of user
 */
@Serializable
data class User(
    override var id: Long,
    override var login: String,
    override var password: String,
    private var orders: ObservableList<Order> = ObservableList(mutableListOf())
) : IUser{
}