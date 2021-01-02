package id.rllyhz.mobilecomputingproject;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import id.rllyhz.mobilecomputingproject.Activity.PlaylistFavSongsActivity;
import id.rllyhz.mobilecomputingproject.Activity.PlaylistSongsActivity;
import id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity;
import id.rllyhz.mobilecomputingproject.Helper.UICustomHelper;

import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.audioIndex;

public class AboutActivity extends AppCompatActivity {
    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        context = AboutActivity.this;

        UICustomHelper.initCustomToolbar(context, getResources().getStringArray(R.array.actionBarTitle)[4], getColor(R.color.titleTextColor));
        UICustomHelper.initMyCustomActionBar(this, R.drawable.toolbar_back_btn, new ColorDrawable(Color.argb(0,0,0,0)));
        UICustomHelper.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(context);
        inflater.inflate(R.menu.my_toolbar_optionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItem_favorit: {
                Intent favoritIntent = new Intent(context, PlaylistFavSongsActivity.class);
                startActivity(favoritIntent);
                finish();
                break;
            }
            case R.id.menuItem_about: {
                Intent aboutIntent = new Intent(context, AboutActivity.class);
                startActivity(aboutIntent);
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigateUp() {
        Intent smartMusicIntent = new Intent(context, SmartMusicPlayerActivity.class);
        startActivity(smartMusicIntent);
        return super.onNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent smartMusicIntent = new Intent(context, SmartMusicPlayerActivity.class);
        startActivity(smartMusicIntent);
    }
}