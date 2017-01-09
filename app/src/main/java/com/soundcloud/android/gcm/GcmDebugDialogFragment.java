package com.soundcloud.android.gcm;


import com.google.firebase.messaging.RemoteMessage;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.java.objects.MoreObjects;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class GcmDebugDialogFragment extends DialogFragment implements GcmMessageHandler.Listener {

    @Inject GcmMessageHandler gcmMessageHandler;
    private TextView debugText;

    public GcmDebugDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.debug_gcm_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        debugText = (TextView) view.findViewById(android.R.id.message);
        gcmMessageHandler.setListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onDestroyView() {
        gcmMessageHandler.setListener(null);
        super.onDestroyView();
    }

    @Override
    public void onRemoteMessage(final RemoteMessage remoteMessage, final String payload) {
        new Handler(Looper.getMainLooper()).post(() -> debugText.append(getMessageOutput(remoteMessage, payload) + "\n\n"));
    }

    private String getMessageOutput(RemoteMessage remoteMessage, String payload) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return MoreObjects.toStringHelper(dateFormat.format(new Date())+ "\n")
                          .add("from", remoteMessage.getFrom())
                          .add("sent_at", remoteMessage.getSentTime())
                          .add("payload", payload)
                          .toString();
    }
}
