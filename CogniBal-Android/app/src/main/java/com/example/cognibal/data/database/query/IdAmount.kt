package com.example.cognibal.data.database.query

/** 换算时一次读出的主键和金额。金额单位是货币的最小单位。 */
data class IdAmount(
    val id: Long,
    val amount: Long,
)
