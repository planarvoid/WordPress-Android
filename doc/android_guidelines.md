# Android conventions

A collection of recommended practices for structuring the codebase and
resources.

## Layout files / activities

If possible the name of the layout file should match the name of the activity /
view.  If you rename the activity, remember to rename the layout file as well.

    FooActivity.java => foo.xml / foo_activity.xml

## Layout file structure

  * No tabs, no trailing whitespace
  * Nested elements should be indented
  * attributes: id first, horizontally aligned
  * No empty bodies

Bad:

    <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
     android:layout_width="fill_parent" android:layout_height="fill_parent"
      android:gravity="center">
      <ProgressBar android:layout_width="wrap_content"
    android:id="@+id/progress"
    android:layout_height="wrap_content" android:layout_margin="10dip"></ProgressBar>
      <ImageView android:id="@+id/image" android:scaleType="centerInside"
        android:layout_height="wrap_content" android:layout_width="wrap_content"></ImageView>
    </RelativeLayout>

Good:

    <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
      android:gravity="center">
      <ProgressBar android:id="@+id/progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="10dip"/>
      <ImageView android:id="@+id/image"
            android:scaleType="centerInside"
            android:layout_height="wrap_content"
            android:layout_width="wrap_content"/>
    </RelativeLayout>
