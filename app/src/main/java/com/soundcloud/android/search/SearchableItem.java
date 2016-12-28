package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;

import android.os.Parcelable;

public interface SearchableItem extends ListItem, Parcelable {

    Urn getUrn();
}
