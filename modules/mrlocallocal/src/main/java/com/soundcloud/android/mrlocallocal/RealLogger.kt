package com.soundcloud.android.mrlocallocal

import android.util.Log
import java.lang.StringBuilder

internal class RealLogger : Logger {
    companion object {
        private val TAG = "MrLocalLocal"
    }
    private val resultOutput = StringBuilder()

    override fun info(message: String) {
        Log.i(TAG, message)
        resultOutput.appendln(message)
    }

    override fun error(message: String) {
        Log.e(TAG, message)
        resultOutput.appendln(message)
    }

    override fun getResultOutput(): String {
        return resultOutput.toString()
    }
}
