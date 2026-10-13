package com.example.cognibal.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 仓库里会碰数据库的挂起函数都从这里进 IO。
 * Room 真正执行 SQL 时用自己的查询线程；已经处在 [androidx.room.withTransaction] 里的语句
 * 会留在事务连接上，不会再抢一个 IO 线程。
 */
internal suspend fun <T> onIo(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
