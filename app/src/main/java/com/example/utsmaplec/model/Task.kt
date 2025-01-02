package com.example.utsmaplec.model

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val completed: Boolean = false
)
