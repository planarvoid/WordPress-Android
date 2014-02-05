package com.soundcloud.android.search;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.FlowLayout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class PlaylistTagsFragment extends Fragment {

    public static final String TAG = "playlist_tags";

    private static final List<String> TAGS = Lists.newArrayList("#chill", "#study", "#indie rock", "#electronic", "#happy", "#party", "#hip hop", "#mashup", "#relax", "#alternative rock", "#piano", "#country", "#morning", "#love", "#punk", "#rock", "#pop", "#house", "#techno", "#rap", "#world", "#dubstep", "#alternative", "#jazz", "#classical", "#soundtrack", "#deep house", "#metal", "#acoustic", "#reggae", "#indie", "#Drum & Bass", "#folk", "#instrumental", "#experimental", "#Progressive House", "#Tech House", "#Electronica", "#pop rock", "#Dance", "#electro", "#ambient", "#trance", "#R&B", "#hardcore", "#Electro House", "#blues", "#remix", "#EDM", "#Electronic House", "#Electronic Pop", "#funk", "#Indie Pop", "#soul", "#trap", "#minimal", "#Singer/Songwriter", "#bass", "#Progressive Rock", "#Mixtape", "#Podcast", "#Progressive", "#Folk Rock", "#Minimal techno", "#bluegrass", "#classic rock", "#club", "#covers", "#disco", "#heavy metal", "#indie folk", "#indie pop", "#latin", "#live", "#lounge", "#mellow", "#motivation", "#music", "#reggaeton", "#running", "#sad", "#sleep", "#trip hop", "#workout");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.playlist_tags_fragment, container, false);
        displayTags(inflater, layout, TAGS);
        return layout;
    }

    private void displayTags(LayoutInflater inflater, View layout, List<String> tags) {
        ViewGroup tagFlowLayout = (ViewGroup) layout.findViewById(R.id.tags);
        tagFlowLayout.removeAllViews();

        int padding = ViewUtils.dpToPx(getActivity(), 10);
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(padding, padding);

        tagFlowLayout.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        for (final String t : tags) {
            if (!TextUtils.isEmpty(t)) {
                TextView txt = ((TextView) inflater.inflate(R.layout.tag_text, null));
                txt.setText(t);
                tagFlowLayout.addView(txt, flowLP);
            }
        }
    }
}
