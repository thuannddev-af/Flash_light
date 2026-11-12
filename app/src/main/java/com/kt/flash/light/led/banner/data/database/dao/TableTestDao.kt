package com.kt.flash.light.led.banner.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kt.flash.light.led.banner.data.database.table.TableTest
import kotlinx.coroutines.flow.Flow

@Dao
interface TableTestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tableTest: TableTest): Long

    @Query("select * from table_test")
    fun getList(): Flow<List<TableTest>>

    @Delete
    fun delete(tableTest: TableTest): Int
}