package com.quartz.powerline.domain.models

import java.util.UUID

data class SearchEngine(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val urlTemplate: String,
    val isEnabled: Boolean = true
)
