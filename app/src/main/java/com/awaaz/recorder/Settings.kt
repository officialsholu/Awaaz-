package com.awaaz.recorder

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog

object Settings {
    private const val FILE = "awaaz_settings"

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun echoOn(c: Context) = prefs(c).getBoolean("echoOn", true)
    fun echoVolPct(c: Context) = prefs(c).getInt("echoVol", 40)
    fun echoDelayMs(c: Context) = prefs(c).getInt("echoDelay", 200)

    fun preset(c: Context): Preset = Preset(
        echoOn(c),
        echoVolPct(c) / 100f,
        echoDelayMs(c),
        0.40f,
        0f,
        0.6f,
        0.30f,
        0.20f
    )

    fun showEchoMenu(a: Activity, done: () -> Unit) {
        val on = echoOn(a)
        val items = arrayOf(
            "Echo effect: " + (if (on) "ON" else "OFF"),
            "Echo volume: " + echoVolPct(a) + "%",
            "Echo delay: " + "%.1f seconds".format(echoDelayMs(a) / 1000.0)
        )
        AlertDialog.Builder(a)
            .setTitle("Echo")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        prefs(a).edit().putBoolean("echoOn", !on).apply()
                        done()
                    }
                    1 -> pickVolume(a, done)
                    2 -> pickDelay(a, done)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    fun pickVolume(a: Activity, done: () -> Unit) {
        val vals = (1..9).map { it * 10 }
        val labels = vals.map { "$it%" }.toTypedArray()
        AlertDialog.Builder(a)
            .setTitle("Echo volume")
            .setSingleChoiceItems(labels, vals.indexOf(echoVolPct(a)).coerceAtLeast(0)) { d, w ->
                prefs(a).edit().putInt("echoVol", vals[w]).apply()
                d.dismiss()
                done()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun pickDelay(a: Activity, done: () -> Unit) {
        val vals = (1..9).map { it * 100 }
        val labels = vals.map { "%.1f seconds".format(it / 1000.0) }.toTypedArray()
        AlertDialog.Builder(a)
            .setTitle("Echo delay")
            .setSingleChoiceItems(labels, vals.indexOf(echoDelayMs(a)).coerceAtLeast(0)) { d, w ->
                prefs(a).edit().putInt("echoDelay", vals[w]).apply()
                d.dismiss()
                done()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
