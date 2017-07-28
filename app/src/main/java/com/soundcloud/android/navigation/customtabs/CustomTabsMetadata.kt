package com.soundcloud.android.navigation.customtabs

import android.net.Uri
import android.support.customtabs.CustomTabsIntent

data class CustomTabsMetadata(val customTabsIntent: CustomTabsIntent,
                              val uri: Uri)
