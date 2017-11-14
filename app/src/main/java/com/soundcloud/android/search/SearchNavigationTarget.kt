package com.soundcloud.android.search

import android.support.v4.app.Fragment
import com.soundcloud.android.R
import com.soundcloud.android.main.BaseNavigationTarget
import com.soundcloud.android.main.Screen
import com.soundcloud.android.search.main.SearchFragment
import com.soundcloud.java.optional.Optional

class SearchNavigationTarget : BaseNavigationTarget(R.string.tab_search, R.drawable.tab_search) {

    override fun createFragment(): Fragment =
        SearchFragment()

    override fun getScreen(): Screen =
        Screen.SEARCH_MAIN

    override fun getPageViewScreen(): Optional<Screen> =
        Optional.absent()
}
