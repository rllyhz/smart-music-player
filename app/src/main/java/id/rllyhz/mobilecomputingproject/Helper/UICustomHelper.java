package id.rllyhz.mobilecomputingproject.Helper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class UICustomHelper {
    private static Toolbar toolbar;
    private static ActionBar actionBar;

    public static void initCustomToolbar(Context context, String title, int color) {
        toolbar = new Toolbar(context);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(color);
        toolbar.setBackground(new ColorDrawable(Color.TRANSPARENT));
    }

    public static void initMyCustomActionBar(AppCompatActivity compatActivity, int indicator, Drawable background) {
        actionBar = compatActivity.getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(UICustomHelper.toolbar);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(indicator);
        actionBar.setBackgroundDrawable(background);
    }

    public static void setDisplayHomeAsUpEnabled(boolean enabled) {
        actionBar.setDisplayHomeAsUpEnabled(enabled);
    }
}
