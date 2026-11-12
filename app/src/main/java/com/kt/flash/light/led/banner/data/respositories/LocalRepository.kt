package com.kt.flash.light.led.banner.data.respositories

import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.data.model.OnBoarding
import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.data.source.LocalDataSource
import javax.inject.Inject

class LocalRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
){
    fun getListLanguage() : List<Language> {
        return localDataSource.getListLanguage()
    }

    fun getList() = localDataSource.getList()
    fun getListOnBoarding(): List<OnBoarding> {
        return localDataSource.getListOnBoarding()
    }
    fun getListPermission(): List<PermissionModel> = localDataSource.getListPermission()

}