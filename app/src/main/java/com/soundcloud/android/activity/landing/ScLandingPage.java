package com.soundcloud.android.activity.landing;

import com.soundcloud.android.activity.create.ScCreate;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;

public interface ScLandingPage {

    enum LandingPage {
        Stream("stream", Stream.class),
        News("news", News.class),
        You("you", You.class),
        Create("create", ScCreate.class),
        Search("search", ScSearch.class),
        Default("default", Stream.class);

        public Class cls;
        public String key;

        LandingPage(String key, Class<? extends ScLandingPage> cls){
            this.key = key;
            this.cls = cls;
        }

        public Intent getIntent(Context c){
            return new Intent(c, cls).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        public static @NotNull LandingPage fromString(String key) {
            for (LandingPage lp : values()) {
                if (lp.key.equals(key)) {
                    return lp;
                }
            }
            return Default;
        }
    }

    LandingPage getPageValue();
}
