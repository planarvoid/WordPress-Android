package com.soundcloud.android.creators

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

const val PACKAGE_NAME_PLAY_STORE: String = "com.android.vending"
const val PACKAGE_NAME_CREATORS: String = "com.soundcloud.creators"
const val PLAY_STORE_URL = "market://details?id=$PACKAGE_NAME_CREATORS"
const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME_CREATORS"

fun isAppInstalled(context: Context, packageName: String): Boolean {
    try {
        context.packageManager.getApplicationInfo(packageName, 0)
        return true
    } catch (e: PackageManager.NameNotFoundException) {
        return false
    }
}

fun isCreatorsAppInstalled(context: Context) = isAppInstalled(context, PACKAGE_NAME_CREATORS)
fun isPlayStoreInstalled(context: Context) = isAppInstalled(context, PACKAGE_NAME_PLAY_STORE)

fun getLaunchIntent(context: Context, packageName: String): Intent? {
    return context.packageManager.getLaunchIntentForPackage(packageName)
}

