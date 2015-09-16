package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class FacebookInvitesItemRenderer implements CellRenderer<FacebookInvitesItem> {

    private final ImageOperations imageOperations;
    private final FacebookInvitesStorage facebookInvitesStorage;

    public interface OnFacebookInvitesClickListener {
        void onFacebookInvitesCloseButtonClicked(int position);

        void onFacebookInvitesInviteButtonClicked(int position);
    }

    private OnFacebookInvitesClickListener onFacebookInvitesClickListener;

    @Inject
    public FacebookInvitesItemRenderer(ImageOperations imageOperations,
                                       FacebookInvitesStorage facebookInvitesStorage) {
        this.imageOperations = imageOperations;
        this.facebookInvitesStorage = facebookInvitesStorage;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.facebook_invites_notification_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<FacebookInvitesItem> notifications) {
        FacebookInvitesItem item = notifications.get(position);

        if (item.hasPictures()) {
            List<String> friendImageUrls = item.getFacebookFriendPictureUrls();
            itemView.findViewById(R.id.friends).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.facebook_invite_introduction_text).setVisibility(View.GONE);
            setFriendImage(itemView, R.id.friend_1, friendImageUrls, 0);
            setFriendImage(itemView, R.id.friend_2, friendImageUrls, 1);
            setFriendImage(itemView, R.id.friend_3, friendImageUrls, 2);
        } else {
            itemView.findViewById(R.id.friends).setVisibility(View.GONE);
            itemView.findViewById(R.id.facebook_invite_introduction_text).setVisibility(View.VISIBLE);
        }

        setClickListeners(itemView, position);
    }

    public void setOnFacebookInvitesClickListener(OnFacebookInvitesClickListener listener) {
        this.onFacebookInvitesClickListener = listener;
    }

    private void setFriendImage(View itemView, int resId, List<String> friendImageUrls, int position) {
        ImageView imageView = (ImageView) itemView.findViewById(resId);

        if (friendImageUrls.size() > position) {
            imageView.setVisibility(View.VISIBLE);
            imageOperations.display(friendImageUrls.get(position), imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private void setClickListeners(View itemView, final int position) {
        itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setDismissed();
                if (onFacebookInvitesClickListener != null) {
                    onFacebookInvitesClickListener.onFacebookInvitesCloseButtonClicked(position);
                }
            }
        });

        itemView.findViewById(R.id.invite_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setClicked();
                if (onFacebookInvitesClickListener != null) {
                    onFacebookInvitesClickListener.onFacebookInvitesInviteButtonClicked(position);
                }
            }
        });
    }
}
