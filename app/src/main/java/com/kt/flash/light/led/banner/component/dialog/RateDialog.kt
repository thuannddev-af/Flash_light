package com.kt.flash.light.led.banner.component.dialog

import android.content.Context
import android.view.LayoutInflater
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.base.dialog.BaseDialog
import com.kt.flash.light.led.banner.databinding.DialogRateBinding

class RateDialog(context: Context, private val onSubmit: (Int) -> Unit) :
    BaseDialog<DialogRateBinding>(context) {

    private var rating = 0
    private val faces = intArrayOf(R.drawable.ic_rate_0, R.drawable.ic_rate_1,
        R.drawable.ic_rate_2, R.drawable.ic_rate_3, R.drawable.ic_rate_4, R.drawable.ic_rate_5)
    private val titles = intArrayOf(R.string.rate_title_0, R.string.rate_title_1,
        R.string.rate_title_2, R.string.rate_title_3, R.string.rate_title_4, R.string.rate_title_5)
    private val messages = intArrayOf(R.string.rate_message_0, R.string.rate_message_1,
        R.string.rate_message_2, R.string.rate_message_3, R.string.rate_message_4, R.string.rate_message_5)

    override fun provideViewBinding() = DialogRateBinding.inflate(LayoutInflater.from(context))

    override fun initViews() = with(viewBinding) {
        closeRate.setOnClickListener { dismiss() }
        listOf(star1, star2, star3, star4, star5).forEachIndexed { index, star ->
            star.setOnClickListener {
                rating = index + 1
                renderRating()
            }
        }
        submitRate.setOnClickListener {
            submitRate.isEnabled = false
            dismiss()
            onSubmit(rating)
        }
        renderRating()
    }

    private fun renderRating() = with(viewBinding) {
        rateFace.setImageResource(faces[rating])
        rateTitle.text = context.getString(titles[rating], context.getString(R.string.app_name))
        rateMessage.setText(messages[rating])
        listOf(star1, star2, star3, star4, star5).forEachIndexed { index, star ->
            star.setImageResource(if (index < rating) R.drawable.ic_star_on else R.drawable.ic_star_off)
            star.isSelected = index < rating
            star.contentDescription = context.getString(R.string.rate_star_description, index + 1)
        }
    }
}
