package com.kt.flash.light.led.banner.component.language

import android.view.LayoutInflater
import android.view.ViewGroup

import com.kt.flash.light.led.banner.base.adapter.BaseAdapter
import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.databinding.ItemLanguageBinding


class LanguageAdapter : BaseAdapter<Language, ItemLanguageBinding>() {
    override fun provideViewBinding(parent: ViewGroup): ItemLanguageBinding {
        return ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun binData(viewBinding: ItemLanguageBinding, item: Language, position: Int) {
        viewBinding.imgLanguage.setImageResource(item.iconRes)
        viewBinding.tvTitle.setText(item.nameRes)
        viewBinding.swLanguage.isChecked = item.selected
        viewBinding.root.setOnClickListener {
            onClick?.invoke(item)
        }
    }

    fun selectLanguage(languageCode: String) {
        var index = dataSet.indexOfFirst { it.selected }
        if (index > -1) {
            dataSet[index].selected = false
            notifyItemChanged(index)
        }
        index = dataSet.indexOfFirst { it.languageCode == languageCode }
        if (index > -1) {
            dataSet[index].selected = true
            notifyItemChanged(index)
        }
    }

    fun selectedLanguage(): Language? = dataSet.firstOrNull { it.selected }
}
