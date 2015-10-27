package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class Shortcut {
    public abstract Urn getUrn();
    public abstract String getDisplayText();

    public static Shortcut create(Urn urn, String displayText){
        return new AutoValue_Shortcut(urn, displayText);
    }
}
