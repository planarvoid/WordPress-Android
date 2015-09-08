package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ShowAllStationsFragment extends LightCycleSupportFragment {

    @Inject @LightCycle ShowAllStationsPresenter presenter;

    public static ShowAllStationsFragment create(int collectionType) {
        final ShowAllStationsFragment fragment = new ShowAllStationsFragment();
        final Bundle bundle = new Bundle();

        bundle.putAll(ShowAllStationsPresenter.createBundle(collectionType));
        fragment.setArguments(bundle);
        return fragment;
    }

    public ShowAllStationsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stations_list, container, false);
    }

}
