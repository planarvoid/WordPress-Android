package com.soundcloud.android.playback.ui;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;

import android.view.View;
import android.view.ViewStub;

@AutoFactory(allowSubclasses = true)
public class EmptyViewController {

    private final View trackView;
    private final TrackPagePresenter.TrackPageHolder holder;

    private final ViewStub emptyLayoutStub;
    private View emptyLayout;

    private boolean isShowing = false;

    EmptyViewController(View trackView) {
        this.trackView = trackView;
        this.holder = (TrackPagePresenter.TrackPageHolder) trackView.getTag();
        this.emptyLayoutStub = (ViewStub) trackView.findViewById(R.id.track_page_empty_stub);
    }

    public void show() {
        isShowing = true;

        setupEmptyLayout();
        holder.waveformController.hide();
        holder.footerUser.setVisibility(View.GONE);
        holder.footerTitle.setText(R.string.playback_empty);
        ViewUtils.setGone(holder.hideOnEmptyViews);
    }

    public void hide() {
        if (isShowing) {
            hideInternal();
            isShowing = false;
        }
    }

    private void hideInternal() {
        holder.waveformController.show();
        holder.footerUser.setVisibility(View.VISIBLE);
        ViewUtils.setVisible(holder.hideOnEmptyViews);
        emptyLayout.setVisibility(View.GONE);
    }

    private void setupEmptyLayout() {
        emptyLayout = trackView.findViewById(R.id.track_page_empty);
        if (emptyLayout == null) {
            emptyLayout = emptyLayoutStub.inflate();
        } else {
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }
}
