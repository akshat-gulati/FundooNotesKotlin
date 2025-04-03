package com.example.fundoonotes.data.repository

import com.example.fundoonotes.data.model.Label

interface LabelRepository {

    fun fetchLabelById(labelId: String, onSuccess: (Label) -> Unit)
    fun fetchLabels()
    fun addNewLabel(labelName: String)
    fun updateLabel(labelId: String, labelName: String)
    fun deleteLabel(labelId: String)

}