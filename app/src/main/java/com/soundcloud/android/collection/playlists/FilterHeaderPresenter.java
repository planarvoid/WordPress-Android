package com.soundcloud.android.collection.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class FilterHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> {

    private final Listener listener;

    @Bind(R.id.btn_filter_options) View optionsButton;
    @Bind(R.id.filter_text) TextView filterText;

    public FilterHeaderPresenter(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        ViewUtils.extendTouchArea(optionsButton);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
    }

    @OnClick(R.id.btn_filter_options)
    public void onClickOptions(View view) {
        listener.onFilterOptionsClicked(view.getContext());
    }

    @OnTextChanged(R.id.filter_text)
    public void onTextChanged(Editable editable) {
        listener.onFilterQuery(editable.toString());
    }

    public void clearFilter() {
        filterText.setText("");
    }

    public interface Listener {
        void onFilterOptionsClicked(Context context);

        void onFilterQuery(String query);
    }
}
