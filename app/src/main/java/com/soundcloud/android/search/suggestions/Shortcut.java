package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class Shortcut extends SearchSuggestion {

    public static Shortcut create(Urn urn, String displayText){
        return new AutoValue_Shortcut(urn,
                displayText,
                Collections.<Map<String,Integer>>emptyList(),
                false);
    }
}
