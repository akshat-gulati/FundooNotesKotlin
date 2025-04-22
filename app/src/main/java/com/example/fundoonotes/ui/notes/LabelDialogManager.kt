package com.example.fundoonotes.ui.notes

import android.content.Context
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.view.ActionMode
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import kotlinx.coroutines.launch

class LabelDialogManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: NoteViewModel
) {
    private val materialCheckBoxes = mutableMapOf<com.google.android.material.checkbox.MaterialCheckBox, String>()

    fun showLabelDialog(mode: ActionMode) {
        // Create Material dialog builder
        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle("Manage Labels")

        // Create dialog content programmatically
        val scrollView = NestedScrollView(context)
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Create container for labels
        val labelsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Loading text
        val loadingText = TextInputEditText(context).apply {
            isEnabled = false
            setText("Loading labels...")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // New label input
        val newLabelInputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            hint = "Create a New Label"
        }

        val newLabelEditText = TextInputEditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Assemble view hierarchy
        newLabelInputLayout.addView(newLabelEditText)
        labelsContainer.addView(loadingText)
        contentLayout.addView(labelsContainer)
        contentLayout.addView(newLabelInputLayout)
        scrollView.addView(contentLayout)
        dialogBuilder.setView(scrollView)

        // Set up dialog buttons
        dialogBuilder.setPositiveButton("Save", null) // We'll override this below
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = dialogBuilder.create()
        dialog.show()

        // We need to override the positive button to prevent automatic dismissal
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val checkedLabelIds = materialCheckBoxes.filter { it.key.isChecked }.map { it.value }
            val uncheckedLabelIds = materialCheckBoxes.filter { !it.key.isChecked }.map { it.value }
            val newLabelName = newLabelEditText.text.toString().trim()
            var newLabelId = ""

            if (newLabelName.isNotEmpty()) {
                newLabelId = viewModel.addNewLabel(newLabelName)
            }

            viewModel.updateSelectedNotesLabels(checkedLabelIds, uncheckedLabelIds, newLabelId)
            dialog.dismiss()
            mode.finish()
        }

        val checkBoxes = mutableMapOf<CheckBox, String>()
        val initialLabelStates = mutableMapOf<String, Boolean>()

        // Fetch labels and populate the dialog
        viewModel.fetchLabels()

        lifecycleOwner.lifecycleScope.launch {
            viewModel.labelDataBridge.labelsState.collect { labels ->
                if (dialog.isShowing) {
                    // Clear existing labels view
                    labelsContainer.removeAllViews()
                    checkBoxes.clear()

                    if (labels.isEmpty()) {
                        val noLabelsText = TextInputEditText(context).apply {
                            isEnabled = false
                            setText("No existing labels. Create one below.")
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }
                        labelsContainer.addView(noLabelsText)
                    } else {
                        labels.forEach { label ->
                            val anyNoteHasLabel = viewModel.selectedNotes.value.any {
                                it.labels.contains(label.id)
                            }
                            initialLabelStates[label.id] = anyNoteHasLabel

                            val checkBox = com.google.android.material.checkbox.MaterialCheckBox(context).apply {
                                text = label.name
                                isChecked = anyNoteHasLabel
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 8, 0, 8)
                                }
                            }
                            materialCheckBoxes[checkBox] = label.id
                            labelsContainer.addView(checkBox)
                        }
                    }
                }
            }
        }
    }
}