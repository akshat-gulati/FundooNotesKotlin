package com.example.fundoonotes.data.model

data class Label(
    val id: String = "",
    val name: String = "",
    val noteIds: List<String> = listOf<String>(),
    val userId: String = ""
)