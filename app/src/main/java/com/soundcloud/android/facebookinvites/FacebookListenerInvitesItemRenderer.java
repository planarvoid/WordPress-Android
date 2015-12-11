package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.StreamDesignExperiment;
import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.android.schedulers.AndroidSchedulers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class FacebookListenerInvitesItemRenderer implements CellRenderer<FacebookInvitesItem> {

    private final ImageOperations imageOperations;
    private final FacebookInvitesStorage facebookInvitesStorage;
    private final FacebookApi facebookApi;
    private final StreamDesignExperiment experiment;

    public interface Listener {
        void onListenerInvitesLoaded(FacebookInvitesItem item);
        void onListenerInvitesDismiss(int position);
        void onListenerInvitesClicked(int position);
    }

    private Listener listener;

    @Inject
    public FacebookListenerInvitesItemRenderer(ImageOperations imageOperations,
                                               FacebookInvitesStorage facebookInvitesStorage,
                                               FacebookApi facebookApi,
                                               StreamDesignExperiment experiment) {
        this.imageOperations = imageOperations;
        this.facebookInvitesStorage = facebookInvitesStorage;
        this.facebookApi = facebookApi;
        this.experiment = experiment;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(
                experiment.isCardDesign() ?
                        R.layout.facebook_invites_notification_card :
                        R.layout.facebook_invites_notification_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, final View itemView, List<FacebookInvitesItem> notifications) {
        final FacebookInvitesItem item = notifications.get(position);
        itemView.setEnabled(false);
        setClickListeners(itemView, position);

        if(item.getFacebookFriendPictureUrls().isPresent()) {
            setContent(itemView, item);
        } else {
            setLoading(itemView);
            facebookApi.friendPictureUrls()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new PictureLoadedSubscriber(itemView, item));
        }
    }

    private void setLoading(View itemView) {
        itemView.findViewById(R.id.loading).setVisibility(View.VISIBLE);
        itemView.findViewById(R.id.content).setVisibility(View.INVISIBLE);
    }

    private void setContent(View itemView, FacebookInvitesItem item) {
        itemView.findViewById(R.id.loading).setVisibility(View.GONE);
        itemView.findViewById(R.id.content).setVisibility(View.VISIBLE);

        if (item.hasPictures()) {
            List<String> friendImageUrls = item.getFacebookFriendPictureUrls().get();
            itemView.findViewById(R.id.friends).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.facebook_invite_introduction_text).setVisibility(View.GONE);
            setFriendImage(itemView, R.id.friend_1, friendImageUrls, 0);
            setFriendImage(itemView, R.id.friend_2, friendImageUrls, 1);
            setFriendImage(itemView, R.id.friend_3, friendImageUrls, 2);
        } else {
            itemView.findViewById(R.id.friends).setVisibility(View.GONE);
            itemView.findViewById(R.id.facebook_invite_introduction_text).setVisibility(View.VISIBLE);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void setFriendImage(View itemView, int resId, List<String> friendImageUrls, int position) {
        ImageView imageView = (ImageView) itemView.findViewById(resId);

        if (friendImageUrls.size() > position) {
            imageView.setVisibility(View.VISIBLE);
            imageOperations.displayCircular(friendImageUrls.get(position), imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private void setClickListeners(View itemView, final int position) {
        itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setDismissed();
                if (listener != null) {
                    listener.onListenerInvitesDismiss(position);
                }
            }
        });

        itemView.findViewById(R.id.invite_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setClicked();
                if (listener != null) {
                    listener.onListenerInvitesClicked(position);
                }
            }
        });
    }

    class PictureLoadedSubscriber extends DefaultSubscriber<List<String>> {

        private final View itemView;
        private final FacebookInvitesItem item;

        PictureLoadedSubscriber(final View itemView, final FacebookInvitesItem item) {
            this.itemView = itemView;
            this.item = item;
        }

        @Override
        public void onNext(List<String> friendPictureUrls) {
            item.setFacebookFriendPictureUrls(friendPictureUrls);
            listener.onListenerInvitesLoaded(item);
            setContent(itemView, item);
        }

        @Override
        public void onError(Throwable e) {
            setContent(itemView, item);
        }

    }
}
