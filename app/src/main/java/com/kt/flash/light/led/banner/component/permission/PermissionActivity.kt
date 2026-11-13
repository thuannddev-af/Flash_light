package com.kt.flash.light.led.banner.component.permission

import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.app.Dialog
import android.net.Uri
import android.provider.Settings
import android.content.ActivityNotFoundException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.kt.flash.light.led.banner.R
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kt.flash.light.led.banner.base.activity.BaseActivity
import com.kt.flash.light.led.banner.component.main.MainActivity
import com.kt.flash.light.led.banner.databinding.ActivityPermissionBinding
import com.kt.flash.light.led.banner.utils.setOnClickWithPressEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {
    private val permissionAdapter = PermissionAdapter()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var requestingPermission = false
    private var navigating = false
    private var settingsDialog: Dialog? = null
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requestingPermission = false
        permissionViewModel.refreshPermissionState()
        if (!granted) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showCameraSettingsDialog()
            } else {
                Toast.makeText(this, R.string.camera_permission_required_to_continue, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun provideViewBinding() = ActivityPermissionBinding.inflate(layoutInflater)

    override fun initViews() = with(viewBaseActivity) {
        rvPermission.adapter = permissionAdapter
        permissionAdapter.onAllowClick = { _, _ -> requestCameraPermission() }
        btnNext.setOnClickWithPressEffect {
            if (ContextCompat.checkSelfPermission(this@PermissionActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@PermissionActivity, R.string.camera_permission_required_to_continue, Toast.LENGTH_LONG).show()
                return@setOnClickWithPressEffect
            }
            if (!navigating) {
                navigating = true
                MainActivity.startNewTask(this@PermissionActivity)
                finish()
            }
        }
    }

    private fun requestCameraPermission() {
        if (requestingPermission || isFinishing || isDestroyed) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionViewModel.refreshPermissionState()
            return
        }
        val preferences = getPreferences(Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_CAMERA_REQUESTED, false) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showCameraSettingsDialog()
            return
        }
        preferences.edit().putBoolean(KEY_CAMERA_REQUESTED, true).apply()
        requestingPermission = true
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showCameraSettingsDialog() {
        if (isFinishing || isDestroyed || settingsDialog?.isShowing == true) return
        settingsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.allow_camera_title)
            .setMessage(R.string.camera_permission_settings_message)
            .setPositiveButton(R.string.no_internet_settings) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.settings_link_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no_internet_cancel, null)
            .show()
    }

    override fun initData() { permissionViewModel.getListPermission() }

    override fun initObserver() {
        permissionViewModel.listPermission.onEach {
            permissionAdapter.submitList(it)
        }.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        permissionViewModel.refreshPermissionState()
    }

    override fun onDestroy() {
        settingsDialog?.dismiss()
        settingsDialog = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_CAMERA_REQUESTED = "camera_permission_requested"
        fun start(context: Context) {
            context.startActivity(Intent(context, PermissionActivity::class.java))
        }
    }
}
