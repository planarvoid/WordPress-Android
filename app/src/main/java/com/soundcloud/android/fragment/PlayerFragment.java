package com.soundcloud.android.fragment;

import com.soundcloud.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PlayerFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize vars and crap
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // setup view here

        ImageView tempPlayer = new ImageView(getActivity());
        tempPlayer.setScaleType(ImageView.ScaleType.CENTER);
        tempPlayer.setImageResource(R.drawable.cloud);

        return tempPlayer;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //add listeners here
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // remove listeners here (I think)
    }
}
