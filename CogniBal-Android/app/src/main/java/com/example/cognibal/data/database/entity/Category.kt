package com.example.cognibal.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cognibal.data.database.model.CategoryType

/**
 * 分类。没有创建时间，字段就按表定义这些。
 *
 * [type] 只有支出、收入。[sort] 越小越靠前，相同排序再按 id。
 */
@Entity(
    tableName = "category",
    indices = [Index(value = ["type", "sort"])],
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val sort: Int,
    val isEnable: Boolean,
)
