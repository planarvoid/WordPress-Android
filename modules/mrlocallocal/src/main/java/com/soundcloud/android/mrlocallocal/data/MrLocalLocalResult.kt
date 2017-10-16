package com.soundcloud.android.mrlocallocal.data

data class MrLocalLocalResult(
        val wasSuccessful: Boolean,
        val canBeRetried: Boolean,
        val message: String,
        val log: String
) {
    companion object {
        @JvmStatic
        fun success(): MrLocalLocalResult {
            return MrLocalLocalResult(true, false, "🚀 MrLocalLocal verification successful!", "")
        }

        @JvmStatic
        fun error(canBeRetried: Boolean, message: String, log: String): MrLocalLocalResult {
            return MrLocalLocalResult(false, canBeRetried, message, log)
        }
    }

    fun buildErrorMessage(): String {
        return """
            $message
            -------------
            $log
            """
    }
}
