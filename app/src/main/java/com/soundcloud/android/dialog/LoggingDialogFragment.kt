package com.soundcloud.android.dialog

import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.util.Log
import com.soundcloud.android.utils.ErrorUtils

open class LoggingDialogFragment : DialogFragment() {

    override fun show(manager: FragmentManager?, tag: String?) {
        ErrorUtils.log(Log.INFO, javaClass.simpleName, "dialog show called")
        super.show(manager, tag)
    }

    override fun show(transaction: FragmentTransaction?, tag: String?): Int {
        ErrorUtils.log(Log.INFO, javaClass.simpleName, "dialog show called")
        return super.show(transaction, tag)
    }

}
