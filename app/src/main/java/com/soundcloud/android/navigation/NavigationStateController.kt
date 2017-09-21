package com.soundcloud.android.navigation

import android.content.SharedPreferences
import com.soundcloud.android.main.Screen
import com.soundcloud.android.storage.StorageModule
import com.soundcloud.android.utils.extensions.put
import com.soundcloud.java.strings.Strings
import javax.inject.Inject
import javax.inject.Named

class NavigationStateController
@Inject constructor(@Named(StorageModule.NAVIGATION_STATE) val sharedPreferences: SharedPreferences) {

    private val stateKey = "NAVIGATION_STATE"

    fun getState(): Screen = Screen.fromTag(sharedPreferences.getString(stateKey, Strings.EMPTY))

    fun setState(state: Screen) {
        sharedPreferences.put(stateKey, state.get())
    }
}
