package com.secnote.pad

import java.io.Serializable

data class NoteMeta(
    val id: String,
    val title: String,
    val algorithm: String,
    val createdAt: Long,
    val modifiedAt: Long
) : Serializable
