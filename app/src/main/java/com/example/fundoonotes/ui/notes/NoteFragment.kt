package com.example.fundoonotes.ui.notes

import NoteAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Note

class NoteFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter

    // Define constants to fix errors
    private val ARG_PARAM1 = "param1"
    private val ARG_PARAM2 = "param2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)

        // First initialize recyclerView before setting its properties
        recyclerView = view.findViewById(R.id.recyclerView)

        // Use GridLayoutManager with 2 columns
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val sampleNotes = listOf(
            Note("Meeting", "Discuss project timeline with the team at 10 AM."),
            Note("Grocery List", "Milk, Eggs, Bread, Butter, Coffee."),
            Note("Workout Plan", "Morning: Cardio. Evening: Strength training."),
            Note("Book to Read", "Start 'Atomic Habits' by James Clear."),
            Note("Weekend Plans", "Visit the beach and try out new café."),
            Note("Coding Task", "Implement RecyclerView adapter in FundooNotes."),
            Note("Doctor's Appointment", "Check-up scheduled for 3:00 PM."),
            Note("Birthday Reminder", "John's birthday on March 25th, buy a gift."),
            Note("Home Maintenance", "Fix the leaking tap in the kitchen."),
            Note("Travel Checklist", "Pack essentials for the upcoming trip.")
        )

        noteAdapter = NoteAdapter(sampleNotes)
        recyclerView.adapter = noteAdapter

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NoteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}