package com.example.fundoonotes.data

import com.example.fundoonotes.data.model.Note

class SampleData {
    private val allNotes = mutableListOf(
        Note(id = "1", title = "Meeting", description = "Discuss project timeline with the team at 10 AM.",
            labels = listOf("Work")),
        Note(id = "2", title = "Grocery List", description = "Milk, Eggs, Bread, Butter, Coffee.",
            labels = listOf("Home")),
        Note(id = "3", title = "Workout Plan", description = "Morning: Cardio. Evening: Strength training.",
            labels = listOf("Health")),
        Note(id = "4", title = "Book to Read", description = "Start 'Atomic Habits' by James Clear.",
            labels = listOf("Personal")),
        Note(id = "5", title = "Weekend Plans", description = "Visit the beach and try out new café.",
            reminderTime = System.currentTimeMillis() + 86400000), // Tomorrow
        Note(id = "6", title = "Coding Task", description = "Implement RecyclerView adapter in FundooNotes.",
            labels = listOf("Work")),
        Note(id = "7", title = "Doctor's Appointment", description = "Check-up scheduled for 3:00 PM.", labels = listOf("Personal"),
            reminderTime = System.currentTimeMillis() + 172800000), // Day after tomorrow
        Note(id = "8", title = "Birthday Reminder", description = "John's birthday on March 25th, buy a gift.",
            reminderTime = System.currentTimeMillis() + 259200000), // Three days from now
        Note(id = "9", title = "Home Maintenance", description = "Fix the leaking tap in the kitchen.",
            isArchived = true, labels = listOf("Home")),
        Note(id = "10", title = "Travel Checklist", description = "Pack essentials for the upcoming trip.",
            isDeleted = true)
    )

    fun getAllNotes(): List<Note> {
        return allNotes
    }
}