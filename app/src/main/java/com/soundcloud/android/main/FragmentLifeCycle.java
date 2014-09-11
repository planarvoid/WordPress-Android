package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public interface FragmentLifeCycle<FragmentT extends Fragment> {
    void onBind(FragmentT owner);
    void onAttach(Activity activity);
    void onCreate(@Nullable Bundle bundle);
    void onViewCreated(View view, @Nullable Bundle savedInstanceState);
    void onStart();
    void onResume();
    void onPause();
    void onStop();
    void onSaveInstanceState(Bundle bundle);
    void onRestoreInstanceState(Bundle bundle);
    void onDestroyView();
    void onDestroy();
    void onDetach();
}
