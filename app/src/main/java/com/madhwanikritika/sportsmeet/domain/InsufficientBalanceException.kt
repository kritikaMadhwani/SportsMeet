package com.madhwanikritika.sportsmeet.domain

class InsufficientBalanceException(
    message: String
) : Exception(message)

class SoldOutException(
    message: String = "This event is sold out."
) : Exception(message)
