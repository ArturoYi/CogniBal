package com.example.cognibal.data.repository

import com.example.cognibal.data.database.model.CategoryType

data class CategoryDraft(
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val sort: Int,
)
