package id.rllyhz.mobilecomputingproject.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;
import id.rllyhz.mobilecomputingproject.AboutActivity;
import id.rllyhz.mobilecomputingproject.Adapter.PlaylistSongAdapter;
import id.rllyhz.mobilecomputingproject.Helper.MusicHelper;
import id.rllyhz.mobilecomputingproject.Helper.PermissionHelper;
import id.rllyhz.mobilecomputingproject.Helper.StorageUtil;
import id.rllyhz.mobilecomputingproject.Helper.UICustomHelper;
import id.rllyhz.mobilecomputingproject.R;
import id.rllyhz.mobilecomputingproject.Service.MediaPlayerService;

import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.activeAudio;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.playerContext;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.songs;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.audioIndex;


public class PlaylistSongsActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout playlistSongLayout, playbackLayout;
    private LinearLayout emptySongsLayout;
    private TextView playbackSongNameTxtView, playbackArtistNameTxtView;
    private ImageView playbackPrevBtn, playbackNextBtn, playbackPlayPauseBtn;
    private CircleImageView playbackAlbumArt;
    private RecyclerView songsListRView;

    // untuk debugging
    private static final String TAG = "PlaylistSongsActivity";

    private Context context;
    private StorageUtil storage;

    PlaylistSongAdapter adapterPlaylistSongs;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_songs);
        context = PlaylistSongsActivity.this;
        storage = new StorageUtil(context);

        PermissionHelper.checkOrRequest(this, this, Manifest.permission.READ_EXTERNAL_STORAGE, PermissionHelper.requestCodeOfReadStorage);
        UICustomHelper.initCustomToolbar(this, getResources().getStringArray(R.array.actionBarTitle)[1], getColor(R.color.titleTextColor));
        UICustomHelper.initMyCustomActionBar(this, R.drawable.toolbar_back_btn , new ColorDrawable(Color.TRANSPARENT));
        UICustomHelper.setDisplayHomeAsUpEnabled(false);

        initAllViews();

        if (songs == null || songs.isEmpty()) {
            playbackLayout.setVisibility(View.GONE);
            emptySongsLayout.setVisibility(View.VISIBLE);
        } else {
            playbackLayout.setVisibility(View.VISIBLE);
            emptySongsLayout.setVisibility(View.GONE);
        }

        setUpViews();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setUpViews();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_toolbar_optionmenu, menu);
        return true;
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
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.playbackLayout || id ==  R.id.playbackAlbumArt) {
            goToMusicPlayerActivity();
        }
    }

    @Override
    public void onBackPressed() {
        goToMusicPlayerActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.requestCodeOfReadStorage) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if (songs == null) {
                    songs = MusicHelper.getSongs(context);
                    storage.storeAudio(songs);
                    goToMusicPlayerActivity();
                }
                showToast("Permission granted!", Toast.LENGTH_SHORT);
            } else {
                showToast("Permission denied!", Toast.LENGTH_SHORT);
            }
        }

        if (requestCode == PermissionHelper.requestCodeOfRecordAudio) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                showToast("Permission granted!", Toast.LENGTH_SHORT);
            } else {
                showToast("Permission denied!", Toast.LENGTH_SHORT);
            }
        }
    }

    private void initAllViews() {
        playlistSongLayout = findViewById(R.id.playlistSongsLayout);
        playbackLayout = findViewById(R.id.playbackLayout);
        emptySongsLayout = findViewById(R.id.emptySongsLayout);
        playbackSongNameTxtView = findViewById(R.id.playbackSongNameTxtView);
        playbackArtistNameTxtView = findViewById(R.id.playbackArtistNameTxtView);
        playbackAlbumArt = findViewById(R.id.playbackAlbumArt);
        playbackPrevBtn = findViewById(R.id.playbackPrevBtn);
        playbackPlayPauseBtn = findViewById(R.id.playbackPlayPauseBtn);
        playbackNextBtn = findViewById(R.id.playbackNextBtn);
        songsListRView = findViewById(R.id.songsListRView);

        // init adapter ke recyclerview
        adapterPlaylistSongs = new PlaylistSongAdapter(context, songs);
        songsListRView.setAdapter(adapterPlaylistSongs);
        songsListRView.setLayoutManager(new LinearLayoutManager(context));

        addClickEventListenerToPlaybackLayoutViews();
    }

    private void addClickEventListenerToPlaybackLayoutViews() {
        playbackLayout.setOnClickListener(this);
        playbackAlbumArt.setOnClickListener(this);

        playbackPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MediaPlayerService.songGetPaused) {
                    playAudio();
                    playbackPlayPauseBtn.setImageResource(R.drawable.notif_pause_btn);
                } else {
                    pauseAudio();
                    playbackPlayPauseBtn.setImageResource(R.drawable.notif_play_btn);
                }
            }
        });

        playbackPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToPrevAudio();
                updateViews();
            }
        });

        playbackNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToNextAudio();
                updateViews();
            }
        });
    }

    private void setUpViews() {
        clearMetaActiveSong();
        updateMetaActiveSong(activeAudio.getTitle(), activeAudio.getArtist(), activeAudio.getAlbumArt());
        updateControllerViews();
    }

    private void updateViews() {
        audioIndex = storage.loadAudioIndex();
        activeAudio = songs.get(audioIndex);

        if (activeAudio.getAlbumArt() == null) {
            activeAudio.setAlbumArt(BitmapFactory.decodeResource(context.getResources(), R.drawable.default_cover_art));
        }

        updateMetaActiveSong(activeAudio.getTitle(), activeAudio.getArtist(), activeAudio.getAlbumArt());
        playbackPlayPauseBtn.setImageResource(R.drawable.notif_pause_btn);
    }

    private void updateMetaActiveSong(String title, String artist, Bitmap albumArt) {
        Log.d(TAG, "updateMetaActiveSong");

        playbackSongNameTxtView.setText(title);
        playbackArtistNameTxtView.setText(artist);

        // cek albumArt
        if (albumArt == null) {
            playbackAlbumArt.setImageResource(R.drawable.default_cover_art);
        } else {
            playbackAlbumArt.setImageBitmap(albumArt);
        }
    }

    private void updateControllerViews() {
        if (MediaPlayerService.songGetPaused) {
            playbackPlayPauseBtn.setImageResource(R.drawable.notif_play_btn);
        } else {
            playbackPlayPauseBtn.setImageResource(R.drawable.notif_pause_btn);
        }
    }

    private void clearMetaActiveSong() {
        Log.d(TAG, "clearMetaActiveSong");

        playbackSongNameTxtView.setText("");
        playbackArtistNameTxtView.setText("");
        playbackPlayPauseBtn.setImageResource(R.drawable.notif_play_btn);
    }

    // navigation methods
    private void goToMusicPlayerActivity() {
        Intent musicPlayerIntent = new Intent(context, SmartMusicPlayerActivity.class);
        musicPlayerIntent.putExtra(SmartMusicPlayerActivity.whichIntentIsCalling, SmartMusicPlayerActivity.PLAYLIST_SONGS_INTENT);
        startActivity(musicPlayerIntent);
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.do_nothing_out_anim);
    }

    // controller methods
    private void playAudio() {
        Intent pauseIntent = new Intent(playerContext, MediaPlayerService.class);
        pauseIntent.setAction(MediaPlayerService.ACTION_PLAY);
        startService(pauseIntent);
    }

    private void pauseAudio() {
        Intent pauseIntent = new Intent(playerContext, MediaPlayerService.class);
        pauseIntent.setAction(MediaPlayerService.ACTION_PAUSE);
        startService(pauseIntent);
    }

    private void skipToPrevAudio() {
        if ( audioIndex ==  0) {
            audioIndex = ( songs.size()-1);
        } else {
            audioIndex = --audioIndex;
        }

        storage.storeAudioIndex(audioIndex);

        sendBroadcast(new Intent(SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO));

        if (MediaPlayerService.songGetPaused) {
            Intent playerService = new Intent(context, MediaPlayerService.class);
            playerService.setAction(MediaPlayerService.ACTION_PLAY);
            startService(playerService);
        } else {
            sendBroadcast(new Intent(SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO));
        }
    }

    private void skipToNextAudio() {
        if ( audioIndex == ( songs.size()-1) ) {
            audioIndex = 0;
        } else {
            audioIndex = ++audioIndex;
        }

        storage.storeAudioIndex(audioIndex);

        if (MediaPlayerService.songGetPaused) {
            Intent playerService = new Intent(context, MediaPlayerService.class);
            playerService.setAction(MediaPlayerService.ACTION_PLAY);
            startService(playerService);
        } else {
            sendBroadcast(new Intent(SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO));
        }
    }


    private void showToast(String message, int length) {
        Toast.makeText(context, message, length).show();
    }
}