package com.soundcloud.android.dialog;


import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.service.sync.SyncOperations;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class OnboardSuggestedUsersSyncFragment extends SherlockFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new SyncOperations(getActivity()).pushFollowings().subscribe(new FollowingsSyncObserver(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_loading_item, container, false);
    }

    private void finish() {
        new SyncStateManager().forceToStale(Content.ME_SOUND_STREAM);
        startActivity(new Intent(Actions.STREAM));
        getActivity().finish();
    }

    private static class FollowingsSyncObserver extends RxFragmentObserver<OnboardSuggestedUsersSyncFragment, Void> {

        public FollowingsSyncObserver(OnboardSuggestedUsersSyncFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(OnboardSuggestedUsersSyncFragment fragment) {
            fragment.finish();
        }

        @Override
        public void onError(OnboardSuggestedUsersSyncFragment fragment, Exception error) {
            Toast.makeText(fragment.getActivity(), "Error pushing some of your followings", Toast.LENGTH_LONG);
        }
    }

}
