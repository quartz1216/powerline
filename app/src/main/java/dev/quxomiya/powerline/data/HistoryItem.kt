package dev.quxomiya.powerline.data

// One app launch from Powerline. Only app launches are recorded: they feed the standby
// "recent apps" row and the frecency bonus in search.
data class HistoryItem(
    val packageName: String,
    val className: String? = null,
    val timestamp: Long
)
