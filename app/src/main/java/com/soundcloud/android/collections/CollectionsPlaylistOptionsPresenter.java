package com.soundcloud.android.collections;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class CollectionsPlaylistOptionsPresenter {

    @Inject
    public CollectionsPlaylistOptionsPresenter() {}

    public interface Listener {
        void onOptionsUpdated(PlaylistsOptions options);
    }

    public void showOptions(Context context, final Listener listener, PlaylistsOptions initialOptions) {
        final View dialoglayout = View.inflate(context, R.layout.dialog_collections_options, null);
        final ToggleButton showLikes = ButterKnife.findById(dialoglayout, R.id.show_likes);
        final ToggleButton showPosts = ButterKnife.findById(dialoglayout, R.id.show_posts);
        final RadioButton sortByTitle = ButterKnife.findById(dialoglayout, R.id.sort_by_title);

        showLikes.setChecked(initialOptions.showLikes());
        showPosts.setChecked(initialOptions.showPosts());
        sortByTitle.setChecked(initialOptions.sortByTitle());

        new AlertDialog.Builder(context)
                .setView(dialoglayout)
                .setPositiveButton(R.string.btn_done, buildFilterListener(listener, showLikes, showPosts, sortByTitle))
                .setNegativeButton(android.R.string.cancel, buildCancelListener())
                .show();
    }

    @NonNull
    private DialogInterface.OnClickListener buildFilterListener(final Listener listener, final ToggleButton showLikes,
                                                                final ToggleButton showPosts, final RadioButton sortByTitle) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final PlaylistsOptions playlistsOptions = AutoValue_PlaylistsOptions.builder()
                        .showLikes(showLikes.isChecked())
                        .showPosts(showPosts.isChecked())
                        .sortByTitle(sortByTitle.isChecked()).build();
                listener.onOptionsUpdated(playlistsOptions);
            }
        };
    }

    @NonNull
    private DialogInterface.OnClickListener buildCancelListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        };
    }

}
