package com.quartz.powerline.domain.models

data class CustomSearchEngine(
    val id: String,
    val name: String,
    val url: String,
    val alias: String? = null,
    val isEnabled: Boolean = true
)
