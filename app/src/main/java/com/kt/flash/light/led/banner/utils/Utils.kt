package com.kt.flash.light.led.banner.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.TextUtils
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale


object Utils {
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun fullScreenImmersive(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showAlertDialog(
        context: Context?,
        strTitle: String?,
        strText: String?,
        buttonOkText: String?,
        buttonCancelText: String?,
        cancelable: Boolean,
        okListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnClickListener?
    ) {
        if (context is Activity && !context.isFinishing) {
            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle(strTitle)
            alertDialogBuilder.setMessage(strText)
            alertDialogBuilder.setPositiveButton(buttonOkText, okListener)
            alertDialogBuilder.setNegativeButton(buttonCancelText, cancelListener)
            alertDialogBuilder.setCancelable(cancelable)
            val alertDialog: Dialog = alertDialogBuilder.create()

            if (!context.isDestroyed) {
                alertDialog.show()
            }
        } else {
            Log.e("showAlertDialog", "Context is not valid or Activity is not running.")
        }
    }

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isPermissionGranted(context: Context?, permission: String?): Boolean {
        return if (context == null || TextUtils.isEmpty(permission)) false else ContextCompat.checkSelfPermission(
            context,
            permission!!
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun formatCurrency(number: Double, currencyCode: String, isRoundDown: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val pattern = if (isRoundDown) "#,##0" else "#,##0.##"
        val decimalFormat = DecimalFormat(pattern, symbols)

        val currencySymbol = try {
            Currency.getInstance(currencyCode.uppercase()).getSymbol(Locale.getDefault())
        } catch (e: Exception) {
            currencyCode.uppercase()
        }

        return  "${decimalFormat.format(number)}$currencySymbol"
    }

}
