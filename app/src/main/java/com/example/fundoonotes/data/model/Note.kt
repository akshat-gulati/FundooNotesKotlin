package com.example.fundoonotes.data.model

//data class Note(
//    val id: String = "",
//    val title: String = "",
//    val content: String = "",
//    val timestamp: Long = 0L,
//    val labels: List<String> = listOf(), // List of label IDs
//    val isArchived: Boolean = false,
//    val isDeleted: Boolean = false,
//    val reminderTime: Long? = null // Optional reminder time
//)
data class Note(
    val title: String,
    val description: String
)