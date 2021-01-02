package id.rllyhz.mobilecomputingproject.Helper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.Model.FavSong;
import id.rllyhz.mobilecomputingproject.R;
import id.rllyhz.mobilecomputingproject.Service.MediaPlayerService;

public class MusicHelper {
    private static final String TAG = "MusicHelper";

    public static ArrayList<Audio> songs = null;
    public static ArrayList<Integer> favSongsIndex = null;
    public static Audio activeAudio = null;
    public static int audioIndex = -1;
    public static int resumePosition = 0;
    public static int MODE_PLAYING;
    public static boolean isFav = false;
    public static boolean isFirstTimeToPlay = true;

    public static Context playerContext;
    public static MediaPlayerService playerServiceInstance;

    public static ArrayList<Audio> getSongs(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
        ArrayList<Audio> audioList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {

            while (cursor.moveToNext()) {

                //String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                String pathId = cursor.getString(column_index);
                Bitmap albumart = null;

                metaRetriver.setDataSource(pathId);
                try {
                    byte[] art = metaRetriver.getEmbeddedPicture();
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inSampleSize = 2;
                    albumart = BitmapFactory.decodeByteArray(art, 0, art.length,opt);
                }
                catch (Exception e)  {
                    Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                }

                if (albumart == null) {
                    albumart = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_background);
                }

                // Save to audioList
                audioList.add(new Audio(data, title, album, albumart, artist, duration, false));
            }
        }
        cursor.close();
        return audioList;
    }

    // Converters
    public static class Converters {

        public static String milliSecondsToTimer(long milliseconds) {
            String finalTimerString = "";
            String secondsString = "";
            String minutesString = "";

            // Convert total duration into time
            int hours = (int) (milliseconds / (1000 * 60 * 60));
            int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
            int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
            // Add hours if there
            if (hours > 0) {
                finalTimerString = hours + ":";
            }

            // Prepending 0 to seconds if it is one digit
            if (seconds < 10) {
                secondsString = "0" + seconds;
            } else {
                secondsString = "" + seconds;
            }

            if (minutes < 10) {
                minutesString = "0" + minutes;
            } else {
                minutesString = "" + minutes;
            }

            finalTimerString = finalTimerString + minutesString + ":" + secondsString;

            // return timer string
            return finalTimerString;
        }

        public static byte[] bitmapToByteArray(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }

        public static Bitmap byteArrayToBitmap(byte[] image) {
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        }

        public static String[] longDurationToString(long milliseconds) {
            String[] minSec = new String[2];

            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
            long seconds = (milliseconds / 1000) % 60;

            if (minutes < 10) {
                minSec[0] = "0" + minutes;
            } else {
                minSec[0] = "" + minutes;
            }

            if (seconds < 10) {
                minSec[1] = "0" + seconds;
            } else {
                minSec[1] = "" + seconds;
            }

            return minSec;
        }

        public static String getDurationFormatted(long milliseconds) {
            if (milliseconds == 0) {
                return "00:00";
            }

            String[] minSec = longDurationToString(milliseconds);

            return minSec[0] + ":" + minSec[1];
        }
    }
}
