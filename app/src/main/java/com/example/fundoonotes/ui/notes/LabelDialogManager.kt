package com.example.fundoonotes.ui.notes

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.fundoonotes.data.model.Note


class LabelDialogManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: NoteViewModel
) {
    fun showLabelDialog(mode: ActionMode) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Manage Labels")

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 16, 24, 0)

        val loadingText = EditText(context)
        loadingText.isEnabled = false
        loadingText.setText("Loading labels...")
        layout.addView(loadingText)

        scrollView.addView(layout)
        dialogBuilder.setView(scrollView)

        val editText = EditText(context)
        editText.hint = "Create a New Label"

        dialogBuilder.setPositiveButton("OK") { _, _ -> }
        dialogBuilder.setNegativeButton("Cancel") { _, _ -> }

        val dialog = dialogBuilder.create()
        dialog.show()

        val checkBoxes = mutableMapOf<CheckBox, String>()
        val initialLabelStates = mutableMapOf<String, Boolean>()

        viewModel.fetchLabels()

        lifecycleOwner.lifecycleScope.launch {
            viewModel.labelDataBridge.labelsState.collect { labels ->
                if (dialog.isShowing) {
                    layout.removeAllViews()
                    checkBoxes.clear()

                    if (labels.isEmpty()) {
                        val noLabelsText = EditText(context)
                        noLabelsText.isEnabled = false
                        noLabelsText.setText("No existing labels. Create one below.")
                        layout.addView(noLabelsText)
                    } else {
                        labels.forEach { label ->
                            val anyNoteHasLabel = viewModel.selectedNotes.value.any {
                                it.labels.contains(label.id)
                            }
                            initialLabelStates[label.id] = anyNoteHasLabel

                            val checkBox = CheckBox(context)
                            checkBox.text = label.name
                            checkBox.isChecked = anyNoteHasLabel
                            checkBoxes[checkBox] = label.id
                            layout.addView(checkBox)
                        }
                    }

                    val paddingView = View(context)
                    paddingView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        24
                    )
                    layout.addView(paddingView)
                    layout.addView(editText)

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val checkedLabelIds = checkBoxes.filter { it.key.isChecked }.map { it.value }
                        val uncheckedLabelIds = checkBoxes.filter { !it.key.isChecked }.map { it.value }
                        val newLabelName = editText.text.toString().trim()
                        var newLabelId = ""

                        if (newLabelName.isNotEmpty()) {
                            newLabelId = viewModel.addNewLabel(newLabelName)
                        }

                        viewModel.updateSelectedNotesLabels(checkedLabelIds, uncheckedLabelIds, newLabelId)
                        dialog.dismiss()
                        mode.finish()
                    }
                }
            }
        }
    }
}