package com.soundcloud.android.view

import android.content.Context
import android.support.v4.content.ContextCompat
import com.mattprecious.telescope.Lens
import com.mattprecious.telescope.TelescopeLayout
import com.soundcloud.android.R
import com.soundcloud.android.utils.BugReporter
import com.soundcloud.java.optional.Optional
import java.io.File

class CustomTelescopeLayout(context: Context, val bugReporter: BugReporter) : TelescopeLayout(context) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLens(BugReportLens(bugReporter, context))
        setProgressColor(ContextCompat.getColor(context, R.color.sc_dark_orange))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        TelescopeLayout.cleanUp(context)
    }

    class BugReportLens(private val bugReporter: BugReporter, private val context: Context) : Lens() {
        override fun onCapture(file: File?) {
            bugReporter.showGeneralFeedbackDialog(context, Optional.fromNullable(file))
        }
    }
}
