package com.kt.flash.light.led.banner.domain.usecase


import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.data.respositories.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListPermissionUseCase @Inject constructor(private val localRepository: LocalRepository) :
    UseCase<GetListPermissionUseCase.Param, List<PermissionModel>>() {
    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<PermissionModel> =
        withContext(Dispatchers.IO) {
            localRepository.getListPermission()
        }
}
