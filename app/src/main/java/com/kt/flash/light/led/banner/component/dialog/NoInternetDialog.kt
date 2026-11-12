package com.kt.flash.light.led.banner.component.dialog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.dialog.BaseDialog
import com.kt.flash.light.led.banner.databinding.DialogNoInternetBinding
import com.kt.flash.light.led.banner.utils.setOnSingleClickListener

class NoInternetDialog(
    context: Context,
    private val onCancelClick: () -> Unit = {}
) : BaseDialog<DialogNoInternetBinding>(context) {
    override fun provideViewBinding() = DialogNoInternetBinding.inflate(LayoutInflater.from(context))

    override fun initViews() {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        viewBinding.btnSettings.setOnSingleClickListener { openNetworkSettings() }
        viewBinding.btnCancel.setOnSingleClickListener {
            dismiss()
            onCancelClick()
        }
    }

    override fun show() {
        super.show()
        window?.apply {
            setLayout((context.resources.displayMetrics.widthPixels * .9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(.6f)
        }
    }

    private fun openNetworkSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            dismiss()
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                dismiss()
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_internet_settings_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
