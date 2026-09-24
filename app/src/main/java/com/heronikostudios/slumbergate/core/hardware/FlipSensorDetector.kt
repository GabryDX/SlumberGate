package com.heronikostudios.slumbergate.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FlipSensorDetector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isProximityNear = false
    private var isFaceDown = false
    private var isSleepStanceActive = false
    private var isAccelerometerRegistered = false
    private var nonFaceDownSampleCount = 0

    var onSleepStanceChanged: ((isFaceDown: Boolean) -> Unit)? = null

    fun startListening() {
        isProximityNear = false
        isFaceDown = false
        isSleepStanceActive = false
        isAccelerometerRegistered = false
        nonFaceDownSampleCount = 0

        // Listen ONLY to proximity sensor initially (event-driven, draws microamps)
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        isAccelerometerRegistered = false
        isSleepStanceActive = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                // If distance is near 0 or less than max range / 5cm threshold
                isProximityNear = distance < maxRange.coerceAtMost(5.0f)

                if (isProximityNear) {
                    // Screen is covered: register accelerometer briefly to verify face-down orientation
                    if (!isAccelerometerRegistered && accelerometer != null) {
                        nonFaceDownSampleCount = 0
                        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                        isAccelerometerRegistered = true
                    }
                } else {
                    // Phone uncovered/picked up: immediately tear down stance and unregister accelerometer
                    isFaceDown = false
                    unregisterAccelerometer()
                    checkStance()
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val zAxis = event.values[2]
                // Face-down when Z acceleration is pointing down (less than -8.5 m/s^2)
                isFaceDown = zAxis < -8.5f
                checkStance()

                if (isFaceDown) {
                    // Confirmed resting face down: unregister accelerometer immediately to prevent continuous CPU wakeups.
                    // The proximity sensor remains active to detect when the phone is lifted.
                    unregisterAccelerometer()
                } else {
                    nonFaceDownSampleCount++
                    // If covered but not face down (e.g. held upright in hand), stop polling after 5 samples (~1 sec)
                    if (nonFaceDownSampleCount >= 5) {
                        unregisterAccelerometer()
                    }
                }
            }
        }
    }

    private fun unregisterAccelerometer() {
        if (isAccelerometerRegistered && accelerometer != null) {
            sensorManager.unregisterListener(this, accelerometer)
            isAccelerometerRegistered = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun checkStance() {
        val inStance = isProximityNear && isFaceDown
        if (inStance && !isSleepStanceActive) {
            isSleepStanceActive = true
            triggerFeedback()
            onSleepStanceChanged?.invoke(true)
        } else if (!inStance && isSleepStanceActive) {
            isSleepStanceActive = false
            onSleepStanceChanged?.invoke(false)
        }
    }

    private fun triggerFeedback() {
        // 10ms haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10L)
                }
            }
        } catch (_: Exception) {
        }

        // Brief audio click
        try {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f)
        } catch (_: Exception) {
        }
    }
}
