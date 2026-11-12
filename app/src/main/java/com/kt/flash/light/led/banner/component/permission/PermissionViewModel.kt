package com.kt.flash.light.led.banner.component.permission

import androidx.lifecycle.viewModelScope
import com.kt.flash.light.led.banner.App
import com.kt.flash.light.led.banner.base.viewmodel.BaseViewModel
import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.domain.usecase.GetListPermissionUseCase
import com.kt.flash.light.led.banner.utils.Logger
import com.kt.flash.light.led.banner.utils.isPermissionsGranted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel
@Inject
constructor(
    private val getListPermissionUseCase: GetListPermissionUseCase,
) : BaseViewModel() {

    private val listPermissionFlow = MutableStateFlow<List<PermissionModel>>(emptyList())
    var listPermission = listPermissionFlow.asStateFlow()

    val isEnableDone: StateFlow<Boolean> = listPermissionFlow
        .map { list -> list.all { it.isAllowed } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun getListPermission() {
        viewModelScope.launch {
            runCatching {
                listPermissionFlow.value =
                    getListPermissionUseCase.execute(
                        GetListPermissionUseCase.Param(),
                    ).map {
                        it.apply {
                            isAllowed = App.context?.isPermissionsGranted(it.permissions) ?: false
                        }
                    }
            }.onFailure { error ->
                Logger.e("getListPermission $error")
            }
        }
    }

    fun changeItemPermission(
        listPermissionModel: List<PermissionModel>,
        position: Int,
        item: PermissionModel,
    ) {
        runCatching {
            val listUpdated =
                listPermissionModel.toMutableList().apply {
                    this[position] = item
                }
            listPermissionFlow.value = listUpdated
        }.onFailure { error ->
            Logger.e("changeItemPermission $error")
        }
    }
}
