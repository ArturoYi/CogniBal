package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.model.CategoryType

data class CategoryDraft(
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val sort: Int,
)
