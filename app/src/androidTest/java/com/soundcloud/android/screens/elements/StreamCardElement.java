package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;

public class StreamCardElement extends Element {

    private static final int CONTAINER = R.id.track_list_item;

    public StreamCardElement(Han testDriver) {
        super(testDriver, With.id(CONTAINER));
    }

    public ProfileScreen clickUserAvatar() {
        userAvatar().click();
        return new ProfileScreen(solo);
    }

    public ProfileScreen clickArtistName() {
        artistName().click();
        return new ProfileScreen(solo);
    }

    public boolean isReposted() {
        return repostItem().isChecked();
    }

    public boolean isLiked() {
        return likeItem().isChecked();
    }

    public StreamScreen toggleRepost() {
        repostItem().click();
        return new StreamScreen(solo);
    }

    public StreamScreen toggleLike() {
        likeItem().click();
        return new StreamScreen(solo);
    }

    public String trackTitle() {
        return new TextElement(solo.findElement(With.id(R.id.title))).getText();
    }

    private ViewElement repostItem() {
        return solo.findElement(With.id(R.id.toggle_repost));
    }

    private ViewElement likeItem() {
        return solo.findElement(With.id(R.id.toggle_like));
    }

    private ViewElement artistName() {
        return solo.findElement(With.id(R.id.creator));
    }

    private ViewElement userAvatar() {
        return solo.findElement(With.id(R.id.user_image));
    }

}
