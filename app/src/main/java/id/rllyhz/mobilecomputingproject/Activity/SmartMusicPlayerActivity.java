package id.rllyhz.mobilecomputingproject.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import id.rllyhz.mobilecomputingproject.AboutActivity;
import id.rllyhz.mobilecomputingproject.Helper.DatabaseHelper;
import id.rllyhz.mobilecomputingproject.Helper.MusicHelper;
import id.rllyhz.mobilecomputingproject.Helper.PermissionHelper;
import id.rllyhz.mobilecomputingproject.Helper.StorageUtil;
import id.rllyhz.mobilecomputingproject.Helper.UICustomHelper;
import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.Model.PlaybackStatus;
import id.rllyhz.mobilecomputingproject.R;
import id.rllyhz.mobilecomputingproject.Service.MediaPlayerService;

import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.favSongsIndex;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.isFav;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.playerServiceInstance;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.songs;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.activeAudio;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.audioIndex;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.MODE_PLAYING;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.resumePosition;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.isFirstTimeToPlay;
import static id.rllyhz.mobilecomputingproject.Service.MediaPlayerService.isSpeechRecognizerActive;

public class SmartMusicPlayerActivity extends AppCompatActivity implements MediaPlayerService.ServiceCallbacks {
    TextView songNameTxtView, artistNameTxtView, timelineTxtView, durationTxtView;
    ImageView prevBtn, nextBtn, playPauseBtn, favoritBtn, repeatShuffleBtn;
    CircleImageView albumArtImgView;
    SeekBar seekBarMusicController;

    SpeechRecognizer speechRecognizer;
    Intent speechIntent;
    String speechResult;

    private final static String TAG = "SmartMusicPlayer";
    public static String whichIntentIsCalling = "whichIntentIsCalling";
    public static int INTENT_CALLER = 0;
    public static int PLAYLIST_SONGS_INTENT = 1;
    public static int PLAYLIST_FAV_SONGS_INTENT = 2;
    public static int NOTIFICATION_PENDING_INTENT = 3;

    private Context context;
    private Context appContext;

    private StorageUtil storage;
    private DatabaseHelper databaseHelper;

    private boolean isFirstTimeAskPermission = true;

    private boolean serviceBound = false;
    private boolean isActivityPaused = true;

    public static final String Broadcast_PLAY_NEW_AUDIO = "id.rllyhz.smartmusikku.action.PlayNewAudio";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            playerServiceInstance = binder.getService();
            playerServiceInstance.setServiceCallbacks(SmartMusicPlayerActivity.this);
            serviceBound = true;

            Log.d(TAG, "connected to the service!");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            playerServiceInstance.setServiceCallbacks(null);
            playerServiceInstance = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_smart_music_player);
        context = SmartMusicPlayerActivity.this;
        appContext = getApplicationContext();
        storage = new StorageUtil(context);
        databaseHelper = new DatabaseHelper(context);
        MusicHelper.playerContext = SmartMusicPlayerActivity.this;
        isActivityPaused = true;

        PermissionHelper.checkOrRequest(context, this, Manifest.permission.READ_EXTERNAL_STORAGE, PermissionHelper.requestCodeOfReadStorage);
        PermissionHelper.checkOrRequest(context, this, Manifest.permission.RECORD_AUDIO, PermissionHelper.requestCodeOfRecordAudio);
        UICustomHelper.initCustomToolbar(context, getResources().getStringArray(R.array.actionBarTitle)[0], getColor(R.color.titleTextColor));
        UICustomHelper.initMyCustomActionBar(this, R.drawable.toolbar_back_btn, new ColorDrawable(Color.argb(0,0,0,0)));

        // cek activity mana yang memanggil
        INTENT_CALLER = getIntent().getIntExtra(whichIntentIsCalling, INTENT_CALLER);

        if (PermissionHelper.check(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            songs = MusicHelper.getSongs(context);
            storage.storeAudio(songs);
        }

        prepareSong();
        initAllViews();
        setUpViews();
        initSpeechRecognizer();

        bindServiceInTheFirstTime();

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        Log.d(TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        isActivityPaused = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        isActivityPaused = true;
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");

        prepareSong();
        initAllViews();
        setUpViews();
        bindServiceInTheFirstTime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityPaused = true;
        Log.d(TAG, "onStop");

        clearMetaActiveSong();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unBindTheService();
        isFirstTimeToPlay = true;
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
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "Back button pressed!");
        Intent playlistSongIntent = new Intent(context, PlaylistSongsActivity.class);
        playlistSongIntent.putExtra("audioIndex", audioIndex);
        startActivity(playlistSongIntent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        return false;
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
                }

                rePrepare();

            } else {
                if (isFirstTimeAskPermission) {
                    String message = "Aplikasi membutuhkan hak akses untuk mengakses penyimpan eksternal anda supaya berjalan dengan lancar. Apakah anda mengizinkannya?";
                    PermissionHelper.showAlert(SmartMusicPlayerActivity.this, context, "Perhatian", message,
                            Manifest.permission.READ_EXTERNAL_STORAGE, PermissionHelper.requestCodeOfReadStorage);

                    isFirstTimeAskPermission = false;
                }
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


    // persiapan untuk data playlist
    private void prepareSong() {
        Log.d(TAG, "prepareSong");

        MODE_PLAYING = storage.getStatusPlayingMode();
        audioIndex = storage.loadAudioIndex();
        favSongsIndex = storage.loadFavSongsIndex();

        if (favSongsIndex != null) {
            isFav = favSongsIndex.contains(audioIndex);
        } else {
            isFav = false;
        }

        if (songs == null) {
            if (!PermissionHelper.check(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showToast("Sistem tidak diperbolehkan mengakses penyimpanan data!", Toast.LENGTH_LONG);
                return;
            }

        } else {
            setActiveAudio(audioIndex);
            saveAudioToPrefs(storage);
        }

        if (MODE_PLAYING == StorageUtil.MODE_NORMAL || MODE_PLAYING == -1) {
            MediaPlayerService.setDefaultPlayMode();
        } else if (MODE_PLAYING == StorageUtil.MODE_REPEAT_SONG) {
            MediaPlayerService.setDefaultPlayMode();
            MediaPlayerService.isRepeat = true;
        } else if (MODE_PLAYING == StorageUtil.MODE_SHUFFLE) {
            MediaPlayerService.setDefaultPlayMode();
            MediaPlayerService.isShuffle = true;
        }
    }

    private void saveAudioToPrefs(StorageUtil storage) {
        Log.d(TAG, "saveAudioToPrefs");

        if (!serviceBound) {
            storage.storeAudio(songs);
        }
        storage.storeAudioIndex(audioIndex);
    }


    // init semua view
    private void initAllViews() {
        Log.d(TAG, "initAllViews");

        albumArtImgView = findViewById(R.id.albumArtImgView);
        songNameTxtView = findViewById(R.id.songNameTxtView);
        artistNameTxtView = findViewById(R.id.artistNameTxtView);
        timelineTxtView = findViewById(R.id.timelineTxtView);
        durationTxtView = findViewById(R.id.durationSongTxtView);
        seekBarMusicController = findViewById(R.id.seekBarMusicController);
        prevBtn = findViewById(R.id.prevBtn);
        nextBtn = findViewById(R.id.nextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        favoritBtn = findViewById(R.id.favoritBtn);
        repeatShuffleBtn = findViewById(R.id.repeatShuffleBtn);

        // tambahkan eventlistener
        addEventListenerToAllViews();
    }

    private void addEventListenerToAllViews() {
        Log.d(TAG, "addEventListenerToAllViews");

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPreparationCompleted() || isSpeechRecognizerActive) return;
                if ( playOrPauseSong() ) isFirstTimeToPlay = false;
            }
        });

        playPauseBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isSpeechRecognizerActive) {
                    startSpeechRecognition();
                    updateModeControllersToModeSpeech();
                    Toast.makeText(context, "Recognition is active", Toast.LENGTH_SHORT).show();
                } else {
                    stopSpeechRecognition();
                    updateModeSpeechToModeControllers();
                    Toast.makeText(context, "Recognition is unactive", Toast.LENGTH_SHORT).show();
                }

                // return true agar tidak tabrakan dengan onClickListener
                return true;
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPreparationCompleted()) return;
                playPrevSong();
                if (isFirstTimeToPlay) isFirstTimeToPlay = false;
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPreparationCompleted()) return;
                playNextSong();
                if (isFirstTimeToPlay) isFirstTimeToPlay = false;
            }
        });

        favoritBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPreparationCompleted()) return;
                addOrRemoveFromFavSong();
            }
        });

        repeatShuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPreparationCompleted()) return;
                setPlayingMode();
            }
        });

        seekBarMusicController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // update timeline
                    timelineTxtView.setText(MusicHelper.Converters.milliSecondsToTimer(progress));

                    if (playerServiceInstance.getMediaPlayer() != null) {
                        playerServiceInstance.seekTo(progress);
                        playerServiceInstance.setResumePosition(progress);
                        resumePosition = progress;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setUpViews() {
        Log.d(TAG, "setUpViews");
        clearMetaActiveSong();

        if (activeAudio == null) {
            updateMetaActiveSong("Tidak Ada Lagu", "-", null, "00:00", 100);
        } else {
            updateMetaActiveSong(activeAudio.getTitle(), activeAudio.getArtist(), activeAudio.getAlbumArt(),
                    MusicHelper.Converters.milliSecondsToTimer(Long.parseLong(activeAudio.getDuration())),
                    Integer.parseInt(activeAudio.getDuration()));
        }

        // update timeline
        seekBarMusicController.setProgress(resumePosition);
        timelineTxtView.setText(MusicHelper.Converters.milliSecondsToTimer(resumePosition));
        updateSeekbarAndTimelineViews(resumePosition);

        setUpControllerViews();
    }

    private void clearMetaActiveSong() {
        Log.d(TAG, "clearMetaActiveSong");

        resumePosition = 0;
        songNameTxtView.setText("");
        artistNameTxtView.setText("");
        durationTxtView.setText("00:00");
        seekBarMusicController.setProgress(0);
        seekBarMusicController.setMax(0);
        albumArtImgView.setImageResource(R.drawable.default_cover_art);
    }

    private void setUpControllerViews() {
        Log.d(TAG, "setUpControllerViews");

        if (MediaPlayerService.songGetPaused) {
            playPauseBtn.setImageResource(R.drawable.controller_play_btn);
        } else {
            playPauseBtn.setImageResource(R.drawable.controller_pause_btn);
        }

        setUpControllersUtilViews();
    }

    private void setUpControllersUtilViews() {
        Log.d(TAG, "setUpControllersUtilViews");

        // cek StatusPlayingMode
        if (MediaPlayerService.isRepeat) {
            repeatShuffleBtn.setImageResource(R.drawable.controller_repeat_song_icon);
        } else if (MediaPlayerService.isShuffle) {
            repeatShuffleBtn.setImageResource(R.drawable.controller_shuffle_icon);
        } else {
            repeatShuffleBtn.setImageResource(R.drawable.controller_unrepeat_icon);
        }

        if (isFav) {
            favoritBtn.setImageResource(R.drawable.controller_fav_icon);
        } else {
            favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
        }
    }

    private boolean isPreparationCompleted() {
        if (!PermissionHelper.check(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showToast("Sistem tidak diperbolehkan mengakses penyimpan data!", Toast.LENGTH_SHORT);
            Log.d(TAG, "Permission to the storage denied! Preparation not completed.");
            return false;
        }

        if (songs != null && songs.size() < 0) {
            showToast("Tidak ada lagu untuk diputar!", Toast.LENGTH_SHORT);
            Log.d(TAG, "Songs not found! Preparation not completed.");
            return false;
        }

        Log.d(TAG, "preparationCompleted");
        return true;
    }


    // update views
    private void updateViews() {
        Log.d(TAG, "updateViews");

        clearMetaActiveSong();

        int indexSong = storage.loadAudioIndex();

        if (indexSong != -1 && indexSong < songs.size()) {
            setActiveAudio(indexSong);
        }

        if (activeAudio == null) {
            updateMetaActiveSong("Tidak Ada Lagu", "-", null, "00:00", 100);
            favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
        } else {
            updateMetaActiveSong(activeAudio.getTitle(), activeAudio.getArtist(), activeAudio.getAlbumArt(),
                    MusicHelper.Converters.milliSecondsToTimer(Long.parseLong(activeAudio.getDuration())), Integer.parseInt(activeAudio.getDuration()));

            if (isFav) {
                favoritBtn.setImageResource(R.drawable.controller_fav_icon);
            } else {
                favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
            }

            favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
        }

    }

    private void updateMetaActiveSong(String title, String artist, Bitmap albumArt, final String duration, int sekbarMax) {
        Log.d(TAG, "updateMetaActiveSong");

        songNameTxtView.setText(title);
        artistNameTxtView.setText(artist);
        durationTxtView.setText(duration);
        seekBarMusicController.setMax(sekbarMax);

        // cek albumArt
        if (albumArt == null) {
            albumArtImgView.setImageResource(R.drawable.default_cover_art);
        } else {
            albumArtImgView.setImageBitmap(albumArt);
        }
    }


    // method untuk fitur favorit
    private void addOrRemoveFromFavSong() {
        if (favSongsIndex == null) favSongsIndex = new ArrayList<>();

        if (!isFav) {
            if (addToFavSong()) {
                Log.d(TAG, "Liked song!");

                favoritBtn.setImageResource(R.drawable.controller_fav_icon);
                showToast("Liked song!", Toast.LENGTH_SHORT);
            }
        } else {
            if (removeFromFavSong()) {
                Log.d(TAG, "Unliked song!");

                favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
                showToast("Unliked song!", Toast.LENGTH_SHORT);
            }
        }
    }

    private boolean removeFromFavSong() {
        int index = favSongsIndex.indexOf(audioIndex);
        if (index != -1) favSongsIndex.remove(index);
        storage.storeFavSongIndex(favSongsIndex);
        isFav = false;
        return true;
    }

    private boolean addToFavSong() {
        favSongsIndex.add(audioIndex);
        storage.storeFavSongIndex(favSongsIndex);
        isFav = true;

        return true;
    }


    // fungsi playlist controller
    private void setPlayingMode() {
        Log.d(TAG, "setPlayingMode");

        if (MediaPlayerService.isRepeat) {
            showToast("Shuffle Mode activated!", Toast.LENGTH_SHORT);
            MediaPlayerService.setDefaultPlayMode();
            MediaPlayerService.isShuffle = true;
            storage.storeStatusPlayingMode(StorageUtil.MODE_SHUFFLE);
            setUpControllersUtilViews();
        } else if (MediaPlayerService.isShuffle) {
            showToast("Normal Mode activated!", Toast.LENGTH_SHORT);
            storage.storeStatusPlayingMode(StorageUtil.MODE_NORMAL);
            MediaPlayerService.setDefaultPlayMode();
            setUpControllersUtilViews();
        } else {
            MediaPlayerService.setDefaultPlayMode();
            MediaPlayerService.isRepeat = true;
            storage.storeStatusPlayingMode(StorageUtil.MODE_REPEAT_SONG);
            setUpControllersUtilViews();
            showToast("Repeat Song activated!", Toast.LENGTH_SHORT);
        }
    }

    private boolean playOrPauseSong() {
        if (MediaPlayerService.songGetPaused) {
            playAudio();
            playPauseBtn.setImageResource(R.drawable.controller_pause_btn);
            return true;
        } else {
            pauseAudio();
            playPauseBtn.setImageResource(R.drawable.controller_play_btn);
            return false;
        }
    }


    // playback methods
    private void playAudio() {
        if (isMyServiceRunning()) {
            Intent playIntent = new Intent(context, MediaPlayerService.class);
            playIntent.setAction(MediaPlayerService.ACTION_PLAY);
            startService(playIntent);

        } else {
            Intent playerIntent = new Intent(context, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void pauseAudio() {
        if (isMyServiceRunning()) {
            Intent pauseIntent = new Intent(context, MediaPlayerService.class);
            pauseIntent.setAction(MediaPlayerService.ACTION_PAUSE);
            startService(pauseIntent);
        }
    }

    private void playNextSong() {
        Intent playPrevSong = new Intent(context, MediaPlayerService.class);
        playPrevSong.setAction(MediaPlayerService.ACTION_PREVIOUS);
        startService(playPrevSong);
    }

    private void playPrevSong() {
        Intent playPrevSong = new Intent(context, MediaPlayerService.class);
        playPrevSong.setAction(MediaPlayerService.ACTION_PREVIOUS);
        startService(playPrevSong);
    }

    private void setActiveAudio(int index) {
        if (index != -1 && index < songs.size()) {
            audioIndex = index;
        } else {
            audioIndex = 0;
        }
        activeAudio = songs.get(audioIndex);
    }


    // feedback UI untuk menampilkan popup
    private void showToast(String message, int length) {
        Toast.makeText(context, message, length).show();
    }


    // fungsi service clients
    private void bindServiceInTheFirstTime() {
        if (!isMyServiceRunning() && !serviceBound) {
            bindService(new Intent(this, MediaPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);

            Log.d(TAG, "Bound to the service!");
        }
    }

    private void unBindTheService() {
        if (serviceBound && isMyServiceRunning()) {
            unbindService(serviceConnection);
            //service aktif
            playerServiceInstance.setServiceCallbacks(null);
            playerServiceInstance.stopSelf();

            Log.d(TAG, "Unbound to the service!");
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (MediaPlayerService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // implementasi fungsi callback
    @Override
    public void updateViewsClient(Audio audio, int songIndex, PlaybackStatus playbackStatus) {
        activeAudio = audio;
        updateViews();

        if (isSpeechRecognizerActive) {
            playPauseBtn.setImageResource(R.drawable.controller_mic_icon);
        }

        if (playbackStatus == PlaybackStatus.PLAYING) {
            playPauseBtn.setImageResource(R.drawable.controller_pause_btn);
        } else {
            playPauseBtn.setImageResource(R.drawable.controller_play_btn);
        }

        if (favSongsIndex != null) {
            if (favSongsIndex.contains(songIndex)) favoritBtn.setImageResource(R.drawable.controller_fav_icon);
        } else {
            favoritBtn.setImageResource(R.drawable.controller_unfav_icon);
        }

        Log.d(TAG, "updateViews from service!");
    }

    @Override
    public void updateSeekbarAndTimelineViews(int position) {
        resumePosition = position;

        new Thread(new Runnable() {
            @Override
            public void run() {
                int total = Integer.parseInt(activeAudio.getDuration());

                while (resumePosition != -1 && resumePosition < total) {
                    if (serviceBound && MediaPlayerService.songGetPaused) {
                        resumePosition = playerServiceInstance.getCurrentPosition();
                        return;
                    }

                    SystemClock.sleep(500);

                    // jika aktivity onPause() atau onStop(), hentikan update UI
                    if (isActivityPaused) return;

                    resumePosition = playerServiceInstance.getCurrentPosition();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // update timeline dan posisi seekbar
                            seekBarMusicController.setProgress(resumePosition);
                            timelineTxtView.setText(MusicHelper.Converters.milliSecondsToTimer(resumePosition));
                        }
                    });

                    Log.d(TAG, "updated timeline and seekbar UI!");
                }
            }
        }).start();
    }


    // methods for recognition
    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000 * 60);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                //Toast.makeText(context, "Ready for speach", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                restartListening();
                switch (error) {
                    case RecognizerIntent.RESULT_AUDIO_ERROR: {
                        //Toast.makeText(context, "RESULT_AUDIO_ERROR", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case RecognizerIntent.RESULT_CLIENT_ERROR: {
                        //Toast.makeText(context, "RESULT_CLIENT_ERROR", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case RecognizerIntent.RESULT_NETWORK_ERROR: {
                        //Toast.makeText(context, "RESULT_NETWORK_ERROR", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case RecognizerIntent.RESULT_NO_MATCH: {
                        //Toast.makeText(context, "RESULT_NO_MATCH", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case RecognizerIntent.RESULT_SERVER_ERROR: {
                        //Toast.makeText(context, "RESULT_SERVER_ERROR", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }

            @Override
            public void onResults(Bundle results) {
                if (results != null && !results.isEmpty() && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION))
                {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    speechResult = matches.get(0);
                    filterResult(speechResult);
                }

                restartListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null) return;

        speechRecognizer.startListening(speechIntent);
        isSpeechRecognizerActive = true;
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer == null) return;

        speechRecognizer.stopListening();
        speechRecognizer.cancel();
        speechRecognizer.destroy();
        isSpeechRecognizerActive = false;
    }

    private void restartListening() {
        stopSpeechRecognition();
        initSpeechRecognizer();
        startSpeechRecognition();
    }

    // untuk memfilter frasa
    private void filterResult(String textResult) {
        textResult = textResult.toLowerCase().trim();

        if ("putar lagu".equals(textResult)) {
            playAudio();
        } else if ("hentikan lagu".equals(textResult)) {
            pauseAudio();
        } else if ("putar selanjutnya".equals(textResult)) {
            playNextSong();
        } else if ("putar sebelumnya".equals(textResult)) {
            playPrevSong();
        }
    }

    private void updateModeControllersToModeSpeech() {
        playPauseBtn.setImageResource(R.drawable.controller_mic_icon);
        nextBtn.setAlpha(0.2f);
        nextBtn.setClickable(false);
        prevBtn.setAlpha(0.2f);
        prevBtn.setClickable(false);
        repeatShuffleBtn.setAlpha(0.2f);
        repeatShuffleBtn.setClickable(false);
        favoritBtn.setAlpha(0.2f);
        favoritBtn.setClickable(false);
    }

    private void updateModeSpeechToModeControllers() {
        nextBtn.setAlpha(1f);
        nextBtn.setClickable(true);
        prevBtn.setAlpha(1f);
        prevBtn.setClickable(true);
        repeatShuffleBtn.setAlpha(1f);
        repeatShuffleBtn.setClickable(true);
        favoritBtn.setAlpha(1f);
        favoritBtn.setClickable(true);

        if (MediaPlayerService.songGetPaused) {
            playPauseBtn.setImageResource(R.drawable.controller_play_btn);
        } else {
            playPauseBtn.setImageResource(R.drawable.controller_pause_btn);
        }
    }


    // fungsi pelengkap
    private void rePrepare() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}