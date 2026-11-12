package com.kt.flash.light.led.banner.component.alertfragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.fragment.BaseFragment
import com.kt.flash.light.led.banner.databinding.FragmentAlertBinding
import com.kt.flash.light.led.banner.utils.setOnSingleClickListener
import java.util.Locale

class AlertFragment : BaseFragment<FragmentAlertBinding>() {
    override fun provideViewBinding(container: ViewGroup?) =
        FragmentAlertBinding.inflate(layoutInflater, container, false)

    override fun initViews() = with(viewBaseFragment) {
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
        currentLanguage.text = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
        itemLanguage.setOnSingleClickListener {
            showUnconfigured()
        }
        itemShare.setOnSingleClickListener {
            val storeUrl = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.settings_share_message, getString(R.string.app_name), storeUrl))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.settings_share_chooser)))
        }
        itemRate.setOnSingleClickListener { openStore() }
        itemPrivacy.setOnSingleClickListener { openPrivacyPolicy() }
        itemPolicy.setOnSingleClickListener { showUnconfigured() }
    }

    private fun openPrivacyPolicy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.settings_link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStore() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.settings_link_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUnconfigured() {
        Toast.makeText(requireContext(), R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://sites.google.com/view/flashlight-led-banner-policy/"
    }

}
