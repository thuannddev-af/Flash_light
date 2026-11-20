package com.kt.flash.light.led.banner.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.kt.flash.light.led.banner.databinding.DialogEdgeDisplayModeBinding
import kotlin.math.roundToInt

internal class EdgeDisplayModeDialog(
    context: Context,
    homeEnabled: Boolean,
    overlayEnabled: Boolean,
    onApply: (Boolean, Boolean) -> Unit
) : Dialog(context) {
    init {
        val binding = DialogEdgeDisplayModeBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.home.isChecked = homeEnabled
        binding.overlay.isChecked = overlayEnabled
        binding.cancel.setOnClickListener { dismiss() }
        binding.apply.setOnClickListener {
            dismiss()
            onApply(binding.home.isChecked, binding.overlay.isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        val metrics = context.resources.displayMetrics
        val width = minOf(metrics.widthPixels - (32 * metrics.density).roundToInt(),
            (360 * metrics.density).roundToInt())
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
