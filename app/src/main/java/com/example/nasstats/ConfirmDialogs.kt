package com.example.nasstats

import android.app.AlertDialog
import android.content.Context

object ConfirmDialogs {

    fun showClearHighsDialog(
        context: Context,
        hotwheelName: String,
        onConfirm: () -> Unit
    ) {

        AlertDialog.Builder(context)
            .setTitle("Clear Highs")
            .setMessage("Clear highs for \"$hotwheelName\"?\n\nThis cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showDeleteDBDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {

        AlertDialog.Builder(context)
            .setTitle("Delete HotWheel")
            .setMessage("Delete Database?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}