package com.example.yogaapp

data class YogaClass(
    val id: Int,
    val day: String,
    val time: String,
    val capacity: Int,
    val duration: Int,
    val price: Double,
    val type: String,
    val description: String?,
    val teacher: String?
)
