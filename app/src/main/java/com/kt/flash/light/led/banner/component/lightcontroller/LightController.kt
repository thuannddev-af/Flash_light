package com.kt.flash.light.led.banner.component.lightcontroller

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.data.model.LightMode

internal class LightController(context: Context, private val onError: (Int) -> Unit) {
    private val appContext = context.applicationContext
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val handler = Handler(Looper.getMainLooper())
    private var cameraId: String? = null
    private var registered = false
    private var ownsTorch = false
    private var active = false
    private var steps = emptyList<LightMode>()
    private var stepIndex = 0

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId && active) fail(R.string.flash_unavailable)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!active || steps.isEmpty()) return
            val step = steps[stepIndex]
            if (!setTorch(step.enabled)) return
            stepIndex = (stepIndex + 1) % steps.size
            handler.postDelayed(this, step.lightms)
        }
    }

    fun register() {
        if (!hasCameraPermission()) return
        if (!registered) {
            manager.registerTorchCallback(callback, handler)
            registered = true
        }
    }

    fun start(mode: Int) {
        stop()
        if (!hasCameraPermission()) {
            onError(R.string.camera_permission_required_to_continue)
            return
        }
        try {
            val available = manager.cameraIdList.filter {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId = available.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: available.firstOrNull()
            if (cameraId == null) {
                onError(R.string.flash_not_supported)
                return
            }
            active = true
            ownsTorch = true
            steps = Light.steps(mode)
            stepIndex = 0
            if (mode == Light.FLASH) setTorch(true) else tick.run()
        } catch (_: CameraAccessException) {
            fail(R.string.flash_unavailable)
        } catch (_: SecurityException) {
            fail(R.string.camera_permission_required_to_continue)
        }
    }

    private fun setTorch(enabled: Boolean): Boolean {
        val id = cameraId ?: return false
        return try {
            manager.setTorchMode(id, enabled)
            true
        } catch (_: CameraAccessException) {
            fail(R.string.flash_unavailable); false
        } catch (_: SecurityException) {
            fail(R.string.camera_permission_required_to_continue); false
        } catch (_: IllegalArgumentException) {
            fail(R.string.flash_not_supported); false
        }
    }

    private fun fail(message: Int) {
        stop()
        onError(message)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun stop() {
        handler.removeCallbacks(tick)
        active = false
        if (ownsTorch) {
            try {
                cameraId?.let { manager.setTorchMode(it, false) }
            } catch (_: CameraAccessException) {
                // The system already disables an unavailable torch.
            } catch (_: SecurityException) {
                // Permission may have been revoked while running.
            } catch (_: IllegalArgumentException) {
                // The camera may have been disconnected.
            }
        }
        ownsTorch = false
        steps = emptyList()
    }

    fun release() {
        stop()
        if (registered) {
            manager.unregisterTorchCallback(callback)
            registered = false
        }
    }
}
