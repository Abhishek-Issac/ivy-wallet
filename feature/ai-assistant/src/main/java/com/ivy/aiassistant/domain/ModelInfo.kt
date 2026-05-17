@file:Suppress("DataClassDefaultValues", "DataClassTypedIDs")

package com.ivy.aiassistant.domain

/**
 * Lightweight metadata about a model returned by a provider's "list models" API.
 */
data class ModelInfo(
    val id: String,
    val ownedBy: String? = null,
)
