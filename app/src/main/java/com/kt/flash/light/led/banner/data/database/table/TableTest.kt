package com.kt.flash.light.led.banner.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_test")
data class TableTest (
    @PrimaryKey
    val id: Int,
    val title: String
)