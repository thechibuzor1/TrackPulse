package com.downbadbuzor.trackpulse.Utils

import android.content.Context
import android.widget.Toast

object UiUtils {
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}