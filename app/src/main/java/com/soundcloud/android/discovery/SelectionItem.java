package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;
import android.view.View.OnClickListener;

@AutoValue
abstract class SelectionItem {
    abstract Optional<Urn> urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<ImageStyle> artworkStyle();

    abstract Optional<Integer> count();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    abstract Optional<String> appLink();

    abstract Optional<String> webLink();

    static SelectionItem create(Optional<Urn> urn,
                                Optional<String> artworkUrlTemplate,
                                Optional<ImageStyle> artworkStyle,
                                Optional<Integer> count,
                                Optional<String> shortTitle,
                                Optional<String> shortSubtitle,
                                Optional<String> appLink,
                                Optional<String> webLink) {
        return new AutoValue_SelectionItem(urn, artworkUrlTemplate, artworkStyle, count, shortTitle, shortSubtitle, appLink, webLink);
    }

    @Nullable
    OnClickListener onClickListener(Navigator navigator) {
        return appLink().or(webLink()).<OnClickListener>transform(
                link -> view -> navigator.openLink(ViewUtils.getFragmentActivity(view), link)).orNull();
    }
}
