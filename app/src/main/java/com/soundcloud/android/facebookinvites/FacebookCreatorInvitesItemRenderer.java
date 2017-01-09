package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class FacebookCreatorInvitesItemRenderer implements CellRenderer<StreamItem.FacebookCreatorInvites> {

    private final ImageOperations imageOperations;
    private final FacebookInvitesStorage facebookInvitesStorage;
    private final EventBus eventBus;
    private Listener listener;

    public interface Listener {
        void onCreatorInvitesDismiss(int position);

        void onCreatorInvitesClicked(int position);
    }

    @Inject
    public FacebookCreatorInvitesItemRenderer(ImageOperations imageOperations,
                                              FacebookInvitesStorage facebookInvitesStorage,
                                              EventBus eventBus) {
        this.imageOperations = imageOperations;
        this.facebookInvitesStorage = facebookInvitesStorage;
        this.eventBus = eventBus;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forCreatorShown());
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.facebook_creator_invites_notification_card, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem.FacebookCreatorInvites> items) {
        ImageView artwork = (ImageView) itemView.findViewById(R.id.artwork);
        Urn trackUrn = items.get(position).trackUrn();
        itemView.setEnabled(false);
        setClickListeners(itemView, position);
        imageOperations.displayWithPlaceholder(trackUrn, ApiImageSize.T300, artwork);
    }

    public void setOnFacebookInvitesClickListener(Listener listener) {
        this.listener = listener;
    }

    private void setClickListeners(View itemView, final int position) {
        itemView.findViewById(R.id.close_button).setOnClickListener(v -> {
            facebookInvitesStorage.setCreatorDismissed();
            if (listener != null) {
                listener.onCreatorInvitesDismiss(position);
            }
        });

        itemView.findViewById(R.id.action_button).setOnClickListener(v -> {
            facebookInvitesStorage.setClicked();
            if (listener != null) {
                listener.onCreatorInvitesClicked(position);
            }
        });
    }
}
