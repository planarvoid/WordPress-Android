package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.R.id.filter_text;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class FilterHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> {

    private final Listener listener;
    private final int hintId;

    @BindView(R.id.btn_filter_options) View optionsButton;
    @BindView(filter_text) TextView filterText;
    private Unbinder unbinder;

    public FilterHeaderPresenter(Listener listener, @StringRes int hintId) {
        this.listener = listener;
        this.hintId = hintId;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        ViewUtils.extendTouchArea(optionsButton);
        ButterKnife.<AppBarLayout>findById(view, R.id.appbar).setExpanded(false);
        filterText.setHint(hintId);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        unbinder.unbind();
        super.onDestroyView(fragment);
    }

    @OnClick(R.id.btn_filter_options)
    public void onClickOptions(View view) {
        listener.onFilterOptionsClicked(view.getContext());
    }

    @OnTextChanged(filter_text)
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
