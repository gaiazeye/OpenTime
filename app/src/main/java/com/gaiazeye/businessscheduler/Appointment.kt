package com.gaiazeye.businessscheduler

data class Appointment(
    val id: String,
    var clientName: String,
    var serviceName: String,
    var date: String,
    var time: String,
    var price: Double
)
