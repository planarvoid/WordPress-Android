package com.soundcloud.android.utils.extensions

import com.soundcloud.android.utils.ScTextUtils
import java.util.Date
import java.util.concurrent.TimeUnit

fun Long.formatTimestamp(): String = ScTextUtils.formatTimestamp(this, TimeUnit.MILLISECONDS)

fun Long.toDate(): Date = Date(this)
