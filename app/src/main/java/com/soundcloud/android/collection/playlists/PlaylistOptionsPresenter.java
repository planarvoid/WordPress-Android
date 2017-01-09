package com.soundcloud.android.collection.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class PlaylistOptionsPresenter {

    private final FeatureOperations featureOperations;

    @Inject
    public PlaylistOptionsPresenter(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    public interface Listener {
        void onOptionsUpdated(PlaylistsOptions options);
    }

    void showOptions(Context context, final Listener listener, PlaylistsOptions initialOptions) {
        final View dialoglayout = View.inflate(context, R.layout.dialog_collections_options, null);
        final ToggleButton showLikes = ButterKnife.findById(dialoglayout, R.id.show_likes);
        final ToggleButton showPosts = ButterKnife.findById(dialoglayout, R.id.show_posts);
        final ToggleButton showOffline = ButterKnife.findById(dialoglayout, R.id.show_offline);
        final RadioButton sortByTitle = ButterKnife.findById(dialoglayout, R.id.sort_by_title);

        showLikes.setChecked(initialOptions.showLikes());
        showPosts.setChecked(initialOptions.showPosts());
        sortByTitle.setChecked(initialOptions.sortByTitle());

        if (featureOperations.isOfflineContentEnabled()) {
            showOffline.setVisibility(View.VISIBLE);
            showOffline.setChecked(initialOptions.showOfflineOnly());
        } else {
            showOffline.setVisibility(View.GONE);
            showOffline.setChecked(false);
        }

        new AlertDialog.Builder(context)
                .setView(dialoglayout)
                .setPositiveButton(R.string.btn_done,
                                   buildFilterListener(listener, showLikes, showPosts, showOffline, sortByTitle))
                .setNegativeButton(android.R.string.cancel, buildCancelListener())
                .show();
    }

    @NonNull
    private DialogInterface.OnClickListener buildFilterListener(final Listener listener,
                                                                final ToggleButton showLikes,
                                                                final ToggleButton showPosts,
                                                                final ToggleButton showOffline,
                                                                final RadioButton sortByTitle) {
        return (dialog, which) -> {
            final PlaylistsOptions playlistsOptions = AutoValue_PlaylistsOptions.builder()
                                                                                .showLikes(showLikes.isChecked())
                                                                                .showPosts(showPosts.isChecked())
                                                                                .sortByTitle(sortByTitle.isChecked())
                                                                                .showOfflineOnly(showOffline.isChecked())
                                                                                .build();
            listener.onOptionsUpdated(playlistsOptions);
        };
    }

    @NonNull
    private DialogInterface.OnClickListener buildCancelListener() {
        return (dialog, which) -> dialog.cancel();
    }

}
