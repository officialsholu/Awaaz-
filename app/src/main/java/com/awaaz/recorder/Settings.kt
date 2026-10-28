package com.awaaz.recorder

import android.content.Context
import android.content.SharedPreferences

object Settings {
    private const val FILE = "awaaz_settings"

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun echoOn(c: Context) = prefs(c).getBoolean("echoOn", true)
    fun echoVol(c: Context) = prefs(c).getInt("echoVol", 35) / 100f
    fun echoDelay(c: Context) = prefs(c).getInt("echoDelay", 250)
    fun echoRepeat(c: Context) = prefs(c).getInt("echoRepeat", 40) / 100f
    fun reverb(c: Context) = prefs(c).getInt("reverb", 30) / 100f
    fun room(c: Context) = prefs(c).getInt("room", 60) / 100f
    fun warmth(c: Context) = prefs(c).getInt("warmth", 30) / 100f
    fun clarity(c: Context) = prefs(c).getInt("clarity", 20) / 100f

    fun preset(c: Context): Preset = Preset(
        echoOn(c), echoVol(c), echoDelay(c), echoRepeat(c),
        reverb(c), room(c), warmth(c), clarity(c)
    )
}
