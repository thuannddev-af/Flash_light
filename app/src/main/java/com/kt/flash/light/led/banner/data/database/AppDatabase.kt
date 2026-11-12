package com.kt.flash.light.led.banner.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kt.flash.light.led.banner.data.database.dao.TableTestDao
import com.kt.flash.light.led.banner.data.database.table.TableTest

@Database(entities = [TableTest::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase(){
    companion object {
        const val  DATABASE_NAME = "FlashLight"
    }
    abstract fun tableTestDao(): TableTestDao
}