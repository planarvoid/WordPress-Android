package com.soundcloud.android.facebookinvites;

import static com.soundcloud.android.stream.StreamItem.forFacebookListenerInvites;

import com.soundcloud.android.R;
import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stream.StreamItem;
import rx.android.schedulers.AndroidSchedulers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.List;

public class FacebookListenerInvitesItemRenderer implements CellRenderer<StreamItem> {

    private final ImageOperations imageOperations;
    private final FacebookInvitesStorage facebookInvitesStorage;
    private final FacebookApi facebookApi;

    public interface Listener {
        void onListenerInvitesLoaded(boolean hasPictures);

        void onListenerInvitesDismiss(int position);

        void onListenerInvitesClicked(int position);
    }

    private Listener listener;

    @Inject
    public FacebookListenerInvitesItemRenderer(ImageOperations imageOperations,
                                               FacebookInvitesStorage facebookInvitesStorage,
                                               FacebookApi facebookApi) {
        this.imageOperations = imageOperations;
        this.facebookInvitesStorage = facebookInvitesStorage;
        this.facebookApi = facebookApi;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.facebook_invites_notification_card, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        final StreamItem.FacebookListenerInvites item = (StreamItem.FacebookListenerInvites) items.get(position);
        itemView.setEnabled(false);
        setClickListeners(itemView, position);

        if (item.friendPictureUrls().isPresent()) {
            setContent(itemView, item);
        } else {
            setLoading(itemView);
            facebookApi.friendPictureUrls()
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(new PictureLoadedSubscriber(itemView, position, items));
        }
    }

    private void setLoading(View itemView) {
        itemView.findViewById(R.id.loading).setVisibility(View.VISIBLE);
        itemView.findViewById(R.id.content).setVisibility(View.INVISIBLE);
    }

    private void setContent(View itemView, StreamItem.FacebookListenerInvites item) {
        itemView.findViewById(R.id.loading).setVisibility(View.GONE);
        itemView.findViewById(R.id.content).setVisibility(View.VISIBLE);

        if (item.hasPictures()) {
            List<String> friendImageUrls = item.friendPictureUrls().get();
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

        itemView.findViewById(R.id.action_button).setOnClickListener(new View.OnClickListener() {
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

        private final WeakReference<View> itemView;
        private final int position;
        private final List<StreamItem> items;

        PictureLoadedSubscriber(final View itemView,
                                final int position,
                                final List<StreamItem> items) {
            this.itemView = new WeakReference<>(itemView);
            this.position = position;
            this.items = items;
        }

        @Override
        public void onNext(List<String> friendPictureUrls) {
            if (itemView.get() != null && listContainsInvitesItem()) {
                final StreamItem.FacebookListenerInvites item = forFacebookListenerInvites(friendPictureUrls);
                items.set(position, item);
                listener.onListenerInvitesLoaded(item.hasPictures());
                setContent(itemView.get(), item);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (itemView.get() != null && listContainsInvitesItem()) {
                setContent(itemView.get(), (StreamItem.FacebookListenerInvites) items.get(position));
            }
        }

        private boolean listContainsInvitesItem() {
            return items.size() > position && items.get(position).kind() == StreamItem.Kind.FACEBOOK_LISTENER_INVITES;
        }
    }
}
