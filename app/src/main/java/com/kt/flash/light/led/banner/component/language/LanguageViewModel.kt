package com.kt.flash.light.led.banner.component.language

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kt.flash.light.led.banner.base.viewmodel.BaseViewModel
import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.domain.usecase.GetListLanguageUseCase

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getListLanguageUseCase: GetListLanguageUseCase
) : BaseViewModel() {
    private val listLanguageFlow = MutableStateFlow<List<Language>>(emptyList())
    var listLanguage = listLanguageFlow.asStateFlow()

    fun loadListLanguage() {
        viewModelScope.launch {
            listLanguageFlow.value = getListLanguageUseCase.execute(GetListLanguageUseCase.Param())
        }
    }
}