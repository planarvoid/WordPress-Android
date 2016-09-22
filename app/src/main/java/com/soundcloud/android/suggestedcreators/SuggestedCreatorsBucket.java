package com.soundcloud.android.suggestedcreators;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
abstract class SuggestedCreatorsBucket {

    public static SuggestedCreatorsBucket create(List<SuggestedCreator> suggestedCreators) {
        return new AutoValue_SuggestedCreatorsBucket(suggestedCreators);
    }

    abstract List<SuggestedCreator> getSuggestedCreators();
}
