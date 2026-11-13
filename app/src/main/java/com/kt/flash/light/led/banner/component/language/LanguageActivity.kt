package com.kt.flash.light.led.banner.component.language

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.component.onboarding.OnboardingActivity
import com.kt.flash.light.led.banner.data.model.Language
import com.kt.flash.light.led.banner.databinding.ActivityLanguageBinding
import com.kt.flash.light.led.banner.utils.Constant
import com.kt.flash.light.led.banner.utils.SpManager
import com.kt.flash.light.led.banner.utils.LocaleHelper
import com.kt.flash.light.led.banner.utils.setOnSingleClickListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    private var selectLanguageModel: Language? = null
    private var saving = false

    private val viewModel: LanguageViewModel by viewModels()

    private val languageAdapter = LanguageAdapter()

    @Inject
    lateinit var  spManager: SpManager

    override fun provideViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(layoutInflater)
    }

    override fun initViews() {
        val isFromSplash = intent.getBooleanExtra(Constant.KEY_INTENT_FROM_SPLASH, false)
        viewBaseActivity.toolBar.btnBack.isVisible = !isFromSplash

        viewBaseActivity.rclLanguage.adapter = languageAdapter
        languageAdapter.onClick = {
            viewBaseActivity.toolBar.btnAction.isVisible = true
            languageAdapter.selectLanguage(it.languageCode)
            languageAdapter.selectedLanguage()?.let { languageModel ->
                selectLanguageModel = languageModel
            }
        }
        viewBaseActivity.toolBar.btnBack.setOnClickListener {
            finish()
        }
        viewBaseActivity.toolBar.tvTitle.text = resources.getString(R.string.settings_language)
        viewBaseActivity.toolBar.btnAction.setText(R.string.save)
        viewBaseActivity.toolBar.btnAction.setOnSingleClickListener {
            saveSelectedLanguage()
        }
    }

    private fun saveSelectedLanguage() {
        val language = selectLanguageModel ?: return
        if (saving || isFinishing || isDestroyed) return
        saving = true

        spManager.saveLanguage(language.copy(languageCode = LocaleHelper.normalizeLanguageCode(language.languageCode)))
        spManager.setLanguageChosen()

        if (intent.getBooleanExtra(Constant.KEY_INTENT_FROM_SPLASH, false)) {
            OnboardingActivity.start(this@LanguageActivity)
        } else {
            // Recreate the whole Main task so every Fragment inflates with the saved locale.
            MainActivity.startNewTask(this)
        }
        finish()
    }


    override fun initData() {
        viewModel.loadListLanguage()
    }

    override fun initObserver() {
        viewModel.listLanguage.onEach {
            languageAdapter.setData(ArrayList(it))
            val savedCode = spManager.getLanguage().languageCode
            languageAdapter.selectLanguage(selectLanguageModel?.languageCode ?: savedCode)
        }.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED).launchIn(lifecycleScope)
    }

    companion object {
        fun start(context: Context, isFromSplash: Boolean) {
            Intent(context, LanguageActivity::class.java).putExtra(Constant.KEY_INTENT_FROM_SPLASH, isFromSplash).also {
                context.startActivity(it)
            }
        }
    }

}
