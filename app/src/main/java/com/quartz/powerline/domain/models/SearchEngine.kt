package com.quartz.powerline.domain.models

import java.util.UUID

data class SearchEngine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val alias: String? = null,
    val isEnabled: Boolean = true
)
