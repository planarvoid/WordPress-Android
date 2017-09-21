package com.soundcloud.android.search.history;

import com.google.auto.value.AutoValue;

@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
@AutoValue
abstract class SearchHistoryDbModel implements SearchHistoryModel {
    
    static final Factory<SearchHistoryDbModel> FACTORY = new Factory<>(AutoValue_SearchHistoryDbModel::new);
}
