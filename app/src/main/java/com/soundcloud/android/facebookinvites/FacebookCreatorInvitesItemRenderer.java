package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class FacebookCreatorInvitesItemRenderer implements CellRenderer<FacebookInvitesItem> {

    private final ImageOperations imageOperations;
    private final FacebookInvitesStorage facebookInvitesStorage;
    private Listener listener;

    public interface Listener {
        void onCreatorInvitesDismiss(int position);

        void onCreatorInvitesClicked(int position);
    }

    @Inject
    public FacebookCreatorInvitesItemRenderer(ImageOperations imageOperations,
                                              FacebookInvitesStorage facebookInvitesStorage) {
        this.imageOperations = imageOperations;
        this.facebookInvitesStorage = facebookInvitesStorage;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.facebook_creator_invites_notification_card, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<FacebookInvitesItem> notifications) {
        ImageView artwork = (ImageView) itemView.findViewById(R.id.artwork);
        FacebookInvitesItem item = notifications.get(position);
        itemView.setEnabled(false);
        setClickListeners(itemView, position);
        imageOperations.displayWithPlaceholder(item.getTrackUrn(), ApiImageSize.T300, artwork);
    }

    public void setOnFacebookInvitesClickListener(Listener listener) {
        this.listener = listener;
    }

    private void setClickListeners(View itemView, final int position) {
        itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setCreatorDismissed();
                if (listener != null) {
                    listener.onCreatorInvitesDismiss(position);
                }
            }
        });

        itemView.findViewById(R.id.invite_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookInvitesStorage.setClicked();
                if (listener != null) {
                    listener.onCreatorInvitesClicked(position);
                }
            }
        });
    }
}
