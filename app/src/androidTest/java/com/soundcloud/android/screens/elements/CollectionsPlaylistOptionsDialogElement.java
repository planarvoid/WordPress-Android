package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class CollectionsPlaylistOptionsDialogElement extends Element {

    public CollectionsPlaylistOptionsDialogElement(Han solo) {
        super(solo, With.id(R.id.collections_playlists_options_dialog));
    }

    public CollectionsPlaylistOptionsDialogElement clickCreated() {
        testDriver.findElement(text(testDriver.getString(R.string.collections_options_toggle_created))).click();
        return this;
    }

    public CollectionsPlaylistOptionsDialogElement clickLiked() {
        testDriver.findElement(text(testDriver.getString(R.string.collections_options_toggle_likes))).click();
        return this;
    }

    public CollectionsPlaylistOptionsDialogElement clickSortByTitle() {
        testDriver.findElement(text(testDriver.getString(R.string.collections_options_dialog_sort_by_title))).click();
        return this;
    }

    public CollectionsPlaylistOptionsDialogElement clickSortByCreatedAt() {
        testDriver.findElement(text(testDriver.getString(R.string.collections_options_dialog_sort_by_creation))).click();
        return this;
    }

    public void clickDone() {
        testDriver.findElement(text(testDriver.getString(R.string.btn_done))).click();
    }
}
