package com.example.fundoonotes.data.model

data class Note(
    val id: String = "",
    val userId: String = "",
    var title: String = "",
    var description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val labels: List<String> = listOf(), // List of label IDs
    var deleted: Boolean = false,
    var archived: Boolean = false,
    val reminderTime: Long? = null, // Optional reminder time
    var deletedTime: Long? = null
)