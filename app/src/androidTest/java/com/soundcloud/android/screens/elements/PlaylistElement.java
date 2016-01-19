package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

public class PlaylistElement {

    private final Han testDriver;
    private final ViewElement wrapped;
    private final int titleId;

    private PlaylistElement(Han testDriver, ViewElement wrapped, int titleId) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
        this.titleId = titleId;
    }

    public static PlaylistElement forListItem(Han testDriver, ViewElement wrapped) {
        return new PlaylistElement(testDriver, wrapped, R.id.list_item_subheader);
    }

    public static PlaylistElement forCard(Han testDriver, ViewElement wrapped) {
        return new PlaylistElement(testDriver, wrapped, R.id.title);
    }

    public boolean isVisible() {
        return wrapped.isVisible();
    }

    public String getTitle() {
        return getText(wrapped.findElement(With.id(titleId)));
    }

    public String getTrackCount() {
        return getText(wrapped.findElement(With.id(R.id.list_item_right_info)));
    }

    public boolean isPromotedPlaylist() {
        return isPromotedCardItem() || isPromotedPlaylistListItem();
    }

    private boolean isPromotedPlaylistListItem() {
        return wrapped.findElement(With.id(R.id.promoted_playlist)).isVisible();
    }

    private boolean isPromotedCardItem() {
        return wrapped.findElement(With.id(R.id.promoted_item)).isVisible();
    }

    public PlaylistDetailsScreen click() {
        wrapped.click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistItemOverflowMenu clickOverflow() {
        wrapped.findElement(With.id(R.id.overflow_button)).click();
        return new PlaylistItemOverflowMenu(testDriver);
    }

    public DownloadImageViewElement downloadElement() {
        return new DownloadImageViewElement(wrapped.findElement(With.id(R.id.item_download_state)));
    }

    private String getText(ViewElement element) {
        return new TextElement(element).getText();
    }

    public static With WithTitle(final Han testDriver, final String title) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return PlaylistElement.forCard(testDriver, view).getTitle().equals(title);
            }

            @Override
            public String getSelector() {
                return String.format("With title: %s", title);
            }
        };
    }

    public static With NotPromoted(final Han testDriver) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return !PlaylistElement.forCard(testDriver, view).isPromotedCardItem();
            }

            @Override
            public String getSelector() {
                return "Not Promoted Playlist";
            }
        };
    }
}
