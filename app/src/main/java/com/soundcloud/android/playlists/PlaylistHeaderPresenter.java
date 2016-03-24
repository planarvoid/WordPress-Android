package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import rx.Subscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class PlaylistHeaderPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    @LightCycle PlaylistEngagementsPresenter engagementsPresenter;

    private final PlaylistDetailsViewFactory playlistDetailsViewFactory;
    private final PlaylistHeaderListener listener;
    private Subscription loadPlaylistSubscription = RxUtils.invalidSubscription();

    private Optional<PlaylistDetailsView> playlistDetailsView = Optional.absent();
    private Optional<PlaylistHeaderItem> playlist = Optional.absent();
    private Fragment fragment;

    public PlaylistHeaderPresenter(@Provided PlaylistDetailsViewFactory playlistDetailsViewFactory,
                                   @Provided PlaylistEngagementsPresenter engagementsPresenter,
                                   PlaylistHeaderListener listener) {
        this.playlistDetailsViewFactory = playlistDetailsViewFactory;
        this.engagementsPresenter = engagementsPresenter;
        this.listener = listener;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        LightCycles.bind(this);
        super.onCreate(fragment, bundle);

        this.fragment = fragment;
    }

    @Override
    public void onViewCreated(final Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final View headerView = view.findViewById(R.id.playlist_details);
        if (headerView != null) {
            setView(headerView);
        }
    }

    public void setView(View view) {
        playlistDetailsView = Optional.of(playlistDetailsViewFactory.create(view,
                getPlayButtonListener(),
                getCreatorClickListener()));

        engagementsPresenter.bindView(view, new OriginProvider() {

            @Override
            public String getScreenTag() {
                return Screen.fromBundle(fragment.getArguments()).get();
            }
        });

        bindPlaylistToView();
    }

    public void setPlaylist(PlaylistHeaderItem playlistItem) {
        this.playlist = Optional.of(playlistItem);
        bindPlaylistToView();
    }

    @NonNull
    private View.OnClickListener getCreatorClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playlist.isPresent()) {
                    listener.onGoToCreator(playlist.get().getCreatorUrn());
                }
            }
        };
    }

    @NonNull
    private View.OnClickListener getPlayButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHeaderPlay();
            }
        };
    }

    private void bindPlaylistToView() {
        if (playlist.isPresent() && playlistDetailsView.isPresent()) {
            final PlaylistHeaderItem playlistHeaderItem = playlist.get();
            playlistDetailsView.get().setPlaylist(playlistHeaderItem, playlistHeaderItem.showPlayButton());
            engagementsPresenter.setPlaylistInfo(playlistHeaderItem);
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        loadPlaylistSubscription.unsubscribe();
        super.onDestroyView(fragment);
    }
}
