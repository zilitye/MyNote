package com.example.mynote.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats a millisecond timestamp as e.g. "Aug 2", used under note/todo titles. */
fun formatDate(timeMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}

/** Formats a millisecond timestamp as e.g. "Aug 2, 5:00 PM", used for todo due dates. */
fun formatDateTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}
