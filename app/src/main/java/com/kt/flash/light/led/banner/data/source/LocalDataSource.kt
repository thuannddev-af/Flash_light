package com.kt.flash.light.led.banner.data.source

import android.os.Build
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.data.database.AppDatabase
import com.kt.flash.light.led.banner.data.database.table.TableTest
import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.data.model.OnBoarding
import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.utils.Permission as PermissionRequest
import javax.inject.Inject
import kotlin.text.insert

class LocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    fun getListLanguage(): List<Language> {
        return listOf(
            Language("en", R.drawable.ic_english, R.string.english),
            Language("es", R.drawable.ic_spanish, R.string.spanish),
            Language("hi", R.drawable.ic_hindi, R.string.hindi),
            Language("ko", R.drawable.ic_korea, R.string.korea),
            Language("ja", R.drawable.ic_japan, R.string.japan),
            Language("de", R.drawable.ic_german,R.string.german),
            Language("pt", R.drawable.ic_portugal, R.string.portuguese),
            Language("fr", R.drawable.ic_french, R.string.french),
            Language("it", R.drawable.ic_italia, R.string.italian),
            Language("id", R.drawable.ic_indo, R.string.indonesia),
            Language("vi", R.drawable.ic_vietnam, R.string.vi_t_nam),
            Language("ru", R.drawable.ic_russia, R.string.russian),
            Language("tr", R.drawable.ic_turkish, R.string.turkish),
            Language("zh", R.drawable.ic_chinese, R.string.chinese)
            )
    }
    fun insert(tableTest: TableTest) {
        appDatabase.tableTestDao().insert(tableTest)
    }

    fun getListOnBoarding(): List<OnBoarding> {
        return listOf(
            OnBoarding(R.drawable.ob1, R.string.bright_flashlight, R.string.turn_your_phone_into_a_powerful_flashlight_anytime),
            OnBoarding(R.drawable.ob2, R.string.sos_dj_flash, R.string.onboarding_sos_description),
            OnBoarding(R.drawable.ob3, R.string.led_banner, R.string.onboarding_led_description),
            OnBoarding(R.drawable.ob4, R.string.screen_light, R.string.onboarding_screen_description)
            )
    }
    fun getList() = appDatabase.tableTestDao().getList()

    fun delete(tableTest: TableTest) {
        appDatabase.tableTestDao().delete(tableTest)
    }
    fun getListPermission(): List<PermissionModel> = listOf(
        PermissionModel(
            R.drawable.ic_permission,
            R.string.allow_camera_title,
            R.string.allow_camera_content,
            listOf(PermissionRequest.CAMERA)
        )
    )
}
