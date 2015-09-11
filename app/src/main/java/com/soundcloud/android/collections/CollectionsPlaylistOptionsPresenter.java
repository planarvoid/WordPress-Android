package com.soundcloud.android.collections;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class CollectionsPlaylistOptionsPresenter {

    @Inject
    public CollectionsPlaylistOptionsPresenter() {
    }

    public interface Listener {
        void onOptionsUpdated(CollectionsOptions options);
    }

    public void showOptions(Context context, final Listener listener, CollectionsOptions initialOptions) {
        final View dialoglayout = View.inflate(context, R.layout.dialog_collections_options, null);

        final ToggleButton showLikes = (ToggleButton) dialoglayout.findViewById(R.id.show_likes);
        final ToggleButton showPosts = (ToggleButton) dialoglayout.findViewById(R.id.show_posts);
        final RadioButton sortByTitle = (RadioButton) dialoglayout.findViewById(R.id.sort_by_title);

        showLikes.setChecked(initialOptions.showLikes());
        showPosts.setChecked(initialOptions.showPosts());
        sortByTitle.setChecked(initialOptions.sortByTitle());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialoglayout);
        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final CollectionsOptions collectionsOptions = AutoValue_CollectionsOptions.builder()
                        .showLikes(showLikes.isChecked())
                        .showPosts(showPosts.isChecked())
                        .sortByTitle(sortByTitle.isChecked()).build();
                listener.onOptionsUpdated(collectionsOptions);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
