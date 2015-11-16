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
    private final ViewElement wrapped;

    public StreamCardElement(Han testDriver, ViewElement wrapped) {
        super(testDriver, With.id(CONTAINER));
        this.wrapped = wrapped;
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
        return new TextElement(wrapped.findElement(With.id(R.id.title))).getText();
    }

    public TrackItemMenuElement clickOverflowButton() {
        overflowButton().click();
        return new TrackItemMenuElement(solo);
    }

    public boolean isPromotedTrack() {
        return wrapped.findElement(With.id(R.id.promoted_item)).isVisible();
    }

    public boolean hasPromoter() {
        return wrapped.findElement(With.id(R.id.promoter)).isVisible();
    }

    public VisualPlayerElement click() {
        wrapped.click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public int height(){
        return wrapped.getHeight();
    }

    private ViewElement repostItem() {
        return wrapped.findElement(With.id(R.id.toggle_repost));
    }

    private ViewElement likeItem() {
        return wrapped.findElement(With.id(R.id.toggle_like));
    }

    private ViewElement artistName() {
        return wrapped.findElement(With.id(R.id.creator));
    }

    private ViewElement userAvatar() {
        return wrapped.findElement(With.id(R.id.user_image));
    }

    private ViewElement overflowButton() {
        return wrapped.findElement(With.id(R.id.overflow_button));
    }
}
