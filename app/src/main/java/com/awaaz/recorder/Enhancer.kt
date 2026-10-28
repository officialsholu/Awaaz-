package com.awaaz.recorder

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

object Enhancer {
    private val keys = listOf("clean", "haram", "masjid", "naat", "custom")
    private val names = listOf("Clean voice", "Haram hall", "Masjid", "Naat studio", "Meri settings")

    fun pick(activity: AppCompatActivity, src: File) {
        AlertDialog.Builder(activity)
            .setTitle("Enhance")
            .setItems(names.toTypedArray()) { _, which ->
                process(activity, src, keys[which])
            }
            .show()
    }

    private fun process(activity: AppCompatActivity, src: File, key: String) {
        Toast.makeText(activity, "Processing...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val base = src.nameWithoutExtension
                    .replace(Regex("_(clean|haram|masjid|naat|custom)[0-9]*$"), "")
                var out = File(src.parentFile, base + "_" + key + ".wav")
                var i = 2
                while (out.exists()) {
                    out = File(src.parentFile, base + "_" + key + i + ".wav")
                    i++
                }
                val preset = if (key == "custom") Settings.preset(activity) else Effects.byName(key)
                Effects.process(src, out, preset)
                activity.runOnUiThread {
                    activity.startActivity(
                        Intent(activity, PlayerActivity::class.java)
                            .putExtra("path", out.absolutePath)
                    )
                    activity.finish()
                }
            } catch (e: Throwable) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Error: " + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
