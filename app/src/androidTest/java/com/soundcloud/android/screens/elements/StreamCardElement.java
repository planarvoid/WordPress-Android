package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;

public class StreamCardElement {

    private final ViewElement wrapped;
    private final Han testDriver;

    public StreamCardElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public ProfileScreen clickUserAvatar() {
        userAvatar().click();
        return new ProfileScreen(testDriver);
    }

    public ProfileScreen clickArtistName() {
        artistName().click();
        return new ProfileScreen(testDriver);
    }

    public boolean hasReposter() {
        return wrapped.isElementOnScreen(With.id(R.id.reposter));
    }

    public boolean isReposted() {
        return repostItem().isChecked();
    }

    public boolean isLiked() {
        return likeItem().isChecked();
    }

    public StreamScreen toggleRepost() {
        repostItem().click();
        return new StreamScreen(testDriver);
    }

    public boolean isTrack() {
        return wrapped.getId() == R.id.track_list_item;
    }

    public boolean isPlaylist() {
        return wrapped.getId() == R.id.playlist_list_item;
    }

    public StreamScreen toggleLike() {
        likeItem().click();
        return new StreamScreen(testDriver);
    }

    public String trackTitle() {
        return new TextElement(wrapped.findOnScreenElement(With.id(R.id.title))).getText();
    }

    public TrackItemMenuElement clickOverflowButton() {
        overflowButton().click();
        return new TrackItemMenuElement(testDriver);
    }

    public boolean isPromotedTrack() {
        return wrapped.isElementOnScreen(With.id(R.id.promoted_item));
    }

    public boolean hasGoIndicator() {
        return wrapped.isElementOnScreen(With.id(R.id.go_indicator));
    }

    public boolean hasPromoter() {
        return wrapped.findOnScreenElement(With.id(R.id.promoter)).isOnScreen();
    }

    public VisualPlayerElement clickToPlay() {
        wrapped.findOnScreenElement(With.id(R.id.title)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public PlaylistDetailsScreen clickToOpenPlaylist() {
        wrapped.findOnScreenElement(With.id(R.id.title)).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    private ViewElement reposter() {
        return wrapped.findOnScreenElement(With.id(R.id.reposter));
    }

    private ViewElement repostItem() {
        return wrapped.findOnScreenElement(With.id(R.id.toggle_repost));
    }

    private ViewElement likeItem() {
        return wrapped.findOnScreenElement(With.id(R.id.toggle_like));
    }

    private ViewElement artistName() {
        return wrapped.findOnScreenElement(With.id(R.id.creator));
    }

    private ViewElement userAvatar() {
        return wrapped.findOnScreenElement(With.id(R.id.user_image));
    }

    private ViewElement overflowButton() {
        return wrapped.findOnScreenElement(With.id(R.id.overflow_button));
    }

    public static With WithGoIndicator(final Han testDriver) {
        return new With() {
            @Override
            public String getSelector() {
                return "With Go indicator";
            }

            @Override
            public boolean apply(ViewElement view) {
                return new StreamCardElement(testDriver, view).hasGoIndicator();
            }
        };
    }
}
