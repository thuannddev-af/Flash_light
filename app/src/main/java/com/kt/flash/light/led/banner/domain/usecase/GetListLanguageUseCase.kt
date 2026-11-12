package com.kt.flash.light.led.banner.domain.usecase

import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.data.respositories.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListLanguageUseCase @Inject constructor(private  val localRepository: LocalRepository) :
    UseCase<GetListLanguageUseCase.Param, List<Language>>()
{
        open class Param: UseCase.Param()

    override suspend fun execute(param: Param): List<Language> = withContext(Dispatchers.IO) {
        localRepository.getListLanguage()
    }
}