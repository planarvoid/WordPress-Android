package com.soundcloud.android.objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FoursquareVenue implements Parcelable {
    public String id, name;
    public List<Category> categories;

    @SuppressWarnings({"UnusedDeclaration"})
    public FoursquareVenue() {}

    public FoursquareVenue(Parcel in) {
        id = in.readString();
        name = in.readString();
        in.readTypedList(new ArrayList<Category>(), Category.CREATOR);
    }

    public Category getCategory() {
        if (categories == null || categories.size() == 0) return null;
        for (Category c : categories) if (c.primary) return c;
        return null;
    }

    public URI getIcon() {
        Category c = getCategory();
        return c == null ? null : c.icon;
    }


    @Override
    public String toString() {
        return "FoursquareVenue{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", categories=" + categories +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeTypedList(categories);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static final Parcelable.Creator<FoursquareVenue> CREATOR
            = new Parcelable.Creator<FoursquareVenue>() {
        public FoursquareVenue createFromParcel(Parcel in) {
            return new FoursquareVenue(in);
        }

        public FoursquareVenue[] newArray(int size) {
            return new FoursquareVenue[size];
        }
    };

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Category implements Parcelable {
        public String id, name;
        public boolean primary;
        public URI icon;

        @SuppressWarnings({"UnusedDeclaration"})
        public Category() {}

        public Category(Parcel in) {
            id = in.readString();
            name = in.readString();
            primary = in.readInt() == 1;
            icon = URI.create(in.readString());
        }

        @Override
        public String toString() {
            return "Category{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", primary=" + primary +
                    ", icon=" + icon +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeString(name);
            dest.writeInt(primary ? 1 : 0);
            dest.writeString(icon.toASCIIString());
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public static final Parcelable.Creator<Category> CREATOR
                = new Parcelable.Creator<Category>() {
            public Category createFromParcel(Parcel in) {
                return new Category(in);
            }

            public Category[] newArray(int size) {
                return new Category[size];
            }
        };
    }
}
