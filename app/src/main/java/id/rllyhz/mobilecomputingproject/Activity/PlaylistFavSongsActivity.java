package id.rllyhz.mobilecomputingproject.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

import id.rllyhz.mobilecomputingproject.AboutActivity;
import id.rllyhz.mobilecomputingproject.Adapter.PlaylistFavSongAdapter;
import id.rllyhz.mobilecomputingproject.Helper.DatabaseHelper;
import id.rllyhz.mobilecomputingproject.Helper.PermissionHelper;
import id.rllyhz.mobilecomputingproject.Helper.UICustomHelper;
import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.R;

import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.favSongsIndex;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.songs;


public class PlaylistFavSongsActivity extends AppCompatActivity {
    private Context context;
    private RecyclerView favSongsListRView;
    private LinearLayout emptySongsLayout;
    private PlaylistFavSongAdapter playlistFavSongAdapter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_fav_songs);
        context = PlaylistFavSongsActivity.this;

        PermissionHelper.checkOrRequest(context, this, Manifest.permission.READ_EXTERNAL_STORAGE, PermissionHelper.requestCodeOfReadStorage);
        UICustomHelper.initCustomToolbar(context, getResources().getStringArray(R.array.actionBarTitle)[2], getColor(R.color.titleTextColor));
        UICustomHelper.initMyCustomActionBar(this, R.drawable.toolbar_back_btn, new ColorDrawable(Color.argb(0,0,0,0)));

        initAllViews();

        if (favSongsIndex == null || favSongsIndex.isEmpty()) {
            favSongsListRView.setVisibility(View.GONE);
            emptySongsLayout.setVisibility(View.VISIBLE);
        } else {
            favSongsListRView.setVisibility(View.VISIBLE);
            emptySongsLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItem_favorit: {
                Intent favoritIntent = new Intent(context, PlaylistFavSongsActivity.class);
                startActivity(favoritIntent);
                break;
            }
            case R.id.menuItem_about: {
                Intent aboutIntent = new Intent(context, AboutActivity.class);
                startActivity(aboutIntent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigateUp() {
        Intent intent = new Intent(context, SmartMusicPlayerActivity.class);
        startActivity(intent);
        return false;
    }

    private void initAllViews() {
        emptySongsLayout = findViewById(R.id.emptySongsLayout);
        favSongsListRView = findViewById(R.id.favSongsListRView);

        final ArrayList<Audio> favSongsList = new ArrayList<>();

        if (favSongsIndex != null && !favSongsIndex.isEmpty()) {
            for (int i = 0; i < favSongsIndex.size(); i++) {
                int index = favSongsIndex.get(i);
                Audio audio = songs.get(index);
                favSongsList.add(audio);
                Log.d("FavSong", "item: " + index);
                Log.d("FavSong", "data: " + audio.getData());
            }
        }

        // init adapter ke recyclerview
        playlistFavSongAdapter = new PlaylistFavSongAdapter(context, favSongsList);
        favSongsListRView.setAdapter(playlistFavSongAdapter);
        favSongsListRView.setLayoutManager(new LinearLayoutManager(context));
    }
}