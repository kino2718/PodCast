package net.kino2718.podcast.utils

import android.util.Log

import net.kino2718.podcast.BuildConfig

object MyLog {
    private const val HEADER = "MyLog: "

    // for error
    fun e(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, HEADER + msg)
        }
    }

    // for warning
    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, HEADER + msg)
        }
    }

    // for information
    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, HEADER + msg)
        }
    }

    // for debug
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, HEADER + msg)
        }
    }
}
