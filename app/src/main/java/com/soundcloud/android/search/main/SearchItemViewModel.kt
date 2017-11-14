package com.soundcloud.android.search.main


sealed class SearchItemViewModel {
    class EmptyCard : SearchItemViewModel()

    class SearchCard : SearchItemViewModel()
}
