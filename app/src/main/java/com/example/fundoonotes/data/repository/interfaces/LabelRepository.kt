package com.example.fundoonotes.data.repository.interfaces

import com.example.fundoonotes.data.model.Label

interface LabelRepository {

    fun fetchLabelById(labelId: String, onSuccess: (Label) -> Unit)
    fun fetchLabels()
    fun addNewLabel(labelName: String): String
    fun updateLabel(labelId: String, labelName: String, noteIds: List<String>)
    fun deleteLabel(labelId: String)

}