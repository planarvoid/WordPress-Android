package com.soundcloud.android.playback.flipper

import android.os.Handler
import android.os.Looper
import com.soundcloud.android.ApplicationModule
import javax.inject.Inject
import javax.inject.Named

class FlipperCallbackHandler
@Inject
constructor(@Named(ApplicationModule.MAIN_LOOPER) looper: Looper) : Handler(looper)
