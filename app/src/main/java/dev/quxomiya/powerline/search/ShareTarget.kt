package dev.quxomiya.powerline.search

import android.graphics.drawable.Drawable

// An app's share (ACTION_SEND) activity reached by an alias: "cl hello" sends "hello" to it
data class ShareTarget(
    val id: String,
    val name: String,
    val alias: String,
    val packageName: String,
    val className: String
)

data class AvailableShareTarget(
    val target: ShareTarget,
    val icon: Drawable? = null
)

// A standby chip: an aliased search engine or a share target. Tapping it types "<alias> ".
data class AliasChip(
    val id: String,
    val name: String,
    val alias: String,
    val icon: Drawable?,
    val isShare: Boolean
)
