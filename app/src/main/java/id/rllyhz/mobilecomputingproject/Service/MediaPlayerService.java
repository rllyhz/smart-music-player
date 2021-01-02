package id.rllyhz.mobilecomputingproject.Service;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity;
import id.rllyhz.mobilecomputingproject.Helper.MusicHelper;
import id.rllyhz.mobilecomputingproject.Helper.PermissionHelper;
import id.rllyhz.mobilecomputingproject.Helper.StorageUtil;
import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.Model.PlaybackStatus;
import id.rllyhz.mobilecomputingproject.R;

import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    // untuk debugging
    private static final String TAG = "MediaPlayerService";

    private Context context;

    // Binder yang akan diberikan ke clients
    private final IBinder iBinder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    // untuk status pause/resume MediaPlayer
    private int resumePosition;

    public static boolean songGetPaused = true;
    public static boolean isRepeat = false;
    public static boolean isShuffle = false;
    public static boolean isSpeechRecognizerActive = false;

    // untuk menanganni panggilan masuk
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    // list untuk menyimpan audio list
    private ArrayList<Audio> audioList;
    private int audioIndex = -1; // index audio yang aktif

    // untk menyimpan audio yang aktif
    private Audio activeAudio;

    public static final String ACTION_PLAY = "id.rllyhz.smartmusikku.ACTION_PLAY";
    public static final String ACTION_PAUSE = "id.rllyhz.smartmusikku.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "id.rllyhz.smartmusikku.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "id.rllyhz.smartmusikku.ACTION_NEXT";
    public static final String ACTION_STOP = "id.rllyhz.smartmusikku.ACTION_STOP";

    // MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    // ID notifikasi
    private static final int NOTIFICATION_ID = 101;

    public static void setDefaultPlayMode() {
        isShuffle = false;
        isRepeat = false;
    }

    private void initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer!");

        mediaPlayer = new MediaPlayer();
        // Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        // Reset agar MediaPlayer tidak mengarah ke data source yang lain
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // set data source yang aktif ke lokasi mediaFile
            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        Log.d(TAG, "playMedia!");
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        Log.d(TAG, "stopMedia!");
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }

        Log.d(TAG, "pauseMedia!");
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }

        Log.d(TAG, "resumeMedia!");
    }

    private void skipToNext() {
        Log.d(TAG, "skipToNext!");

        if (audioIndex == audioList.size() - 1) {
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            int index = ++audioIndex;
            activeAudio = audioList.get(index);
        }

        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        // reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        Log.d(TAG, "skipToPrevious!");

        if (audioIndex == 0) {
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            int index = --audioIndex;
            activeAudio = audioList.get(index);
        }

        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        // reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void initMediaSession() throws RemoteException {
        Log.d(TAG, "initMediaSession!");

        // mediaSessionManager harus tersedia di perangkat pengguna
        if (mediaSessionManager != null) return;

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // buat MediaSession baru
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        // init transport controls dari MediaSessions
        transportControls = mediaSession.getController().getTransportControls();
        // set MediaSession -> agar siap menerima perintah
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // update metadata
        updateMetaData();

        // set callback
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                // hentikan service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Log.d(TAG, "updateMetaData");

        // Update metadata ke audio aktif
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, activeAudio.getAlbumArt())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast received!");

            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            audioList = new StorageUtil(getApplicationContext()).loadAudio();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            stopMedia();
            if (mediaPlayer != null) mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        Log.d(TAG, "Registering Receiver: " + SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO);

        IntentFilter filter = new IntentFilter(SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        Log.d(TAG, "Registering Receiver: " + AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

        Log.d(TAG,PhoneStateListener.LISTEN_CALL_STATE + " listener registered!");
    }

    private void updateUIClient(PlaybackStatus playbackStatus) {
        if (playbackStatus == PlaybackStatus.PLAYING) {
            if (serviceCallbacks != null) {
                serviceCallbacks.updateSeekbarAndTimelineViews(resumePosition);
                serviceCallbacks.updateViewsClient(activeAudio, audioIndex, PlaybackStatus.PLAYING);
            }
        }

        MusicHelper.resumePosition = resumePosition;
        MusicHelper.audioIndex = audioIndex;
        MusicHelper.activeAudio = activeAudio;
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent play_pauseAction = null;

        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            play_pauseAction = playbackAction(1);
            songGetPaused = false;
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            play_pauseAction = playbackAction(0);
            songGetPaused = true;
        }

        Intent resultIntent = new Intent(this, SmartMusicPlayerActivity.class);
        resultIntent.putExtra(SmartMusicPlayerActivity.whichIntentIsCalling, SmartMusicPlayerActivity.NOTIFICATION_PENDING_INTENT);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0, resultIntent, PendingIntent.FLAG_ONE_SHOT);

        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //stackBuilder.addNextIntentWithParentStack(resultIntent);
        //PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // cek jika albumArt tidak ada
        Bitmap albumArt;

        if (activeAudio.getAlbumArt() == null) {
            albumArt = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_cover_art);
        } else {
            albumArt = activeAudio.getAlbumArt();
        }

        // buat notifikasi
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setShowWhen(false)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setLargeIcon(albumArt)
                .setContentText(activeAudio.getArtist())
                .setContentTitle(activeAudio.getAlbum())
                .setContentInfo(activeAudio.getTitle())
                .setContentIntent(resultPendingIntent)
                // tambahkan tombol untuk mengontrol playback
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        // start foreground untuk notifikasi service
        // agar service tidak di 'destroy' oleh system ketika tidak ada task yang dijalankan
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        // ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());

        updateUIClient(playbackStatus);

        Log.d(TAG, "Notification showing!");
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void handleIncomingActions(Intent playbackAction) {
        Log.d(TAG, "handleIncomingActions");

        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();

        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            songGetPaused = false;
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            songGetPaused = true;
            MusicHelper.resumePosition = resumePosition;
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
            songGetPaused = false;
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            songGetPaused = false;
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // putar
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // jeda
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // skip ke selanjutnya
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // skip ke sebelumnya
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) { }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");

        resumePosition = 0;

        // ketika lagu selesai diputar
        // cek apakah pengguna ingin mengulang lagu
        if (isRepeat) {
            // putar lagu dengan index yang sama
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);

        } else if (isShuffle) {
            // jika ingin diacak, set terlebih dahulu index ke angka acak
            // antara index yang pertama (0) hingga yang terakhir yang ada playlist
            int randomIndex = getRandomNumber(0, audioList.size()-1);

            // simpan ke sharedPrefferences
            StorageUtil storage = new StorageUtil(context);
            storage.storeAudioIndex(randomIndex);

            // kirim broadcast
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);

            updateUIClient(PlaybackStatus.PLAYING);

        } else {
            // jika mode playing adalah default, putar lagu selanjutnya
            // kirim broadcast
            skipToNext();

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);

            updateUIClient(PlaybackStatus.PLAYING);
        }

        if (serviceCallbacks != null) {
            serviceCallbacks.updateSeekbarAndTimelineViews(0);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.e(TAG, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e(TAG, "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e(TAG, "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete");
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        if (isSpeechRecognizerActive) return;

        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isSpeechRecognizerActive) break;
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }

    private void removeAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        try {
            //ambil data dari SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        // jika tidak, lanjutkan seperti biasa

        if (requestAudioFocus() == false) {
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        // handle intent (perintah) yang dikirim oleh client
        handleIncomingActions(intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        context = MediaPlayerService.this;

        callStateListener();
        registerBecomingNoisyReceiver();
        register_playNewAudio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister semua BroadcastReceiver
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }


    // binder untuk memberikan akses terhadap instance (objek) dari service
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    // interface interface untuk berkomunikasi ke clients
    public interface ServiceCallbacks {
        void updateViewsClient(Audio audioActive, int audioIndex, PlaybackStatus playbackStatus);
        void updateSeekbarAndTimelineViews(int position);
    }

    public void setServiceCallbacks(ServiceCallbacks serviceCallbacks) {
        this.serviceCallbacks = serviceCallbacks;

        Log.d(TAG, "ServiceCallbacks was set up!");
    }

    private int getRandomNumber(int minimum, int maximum) {
        return minimum + (int)(Math.random() * maximum);
    }


    // fungsi-fungsi yang bisa diakses client
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setResumePosition(int position) {
        this.resumePosition = position;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }
}


