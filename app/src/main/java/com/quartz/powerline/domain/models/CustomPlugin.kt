package com.quartz.powerline.domain.models

enum class PluginAction {
    OPEN_URL,
    COPY_TO_CLIPBOARD
}

data class CustomPlugin(
    val id: String,
    val name: String,
    val keyword: String,
    val action: PluginAction,
    val actionData: String
)
