package id.rllyhz.mobilecomputingproject.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.Model.FavSong;


public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Smart_musik.db";
    public static final int VERSION = 1;
    public static final String FAV_SONGS_TABLE = "fav_songs_table";

    public static final String ID_COLUMN = "ID";
    public static final String INDEX_COLUMN = "INDEX";
    public static final String DATA_COLUMN = "DATA";

    private SQLiteDatabase database;
    private Cursor cursor;
    private ContentValues contentValues;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + FAV_SONGS_TABLE + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, INDEX_ID INTEGER, DATA TEXT, TITLE TEXT, ARTIST TEXT, DURATION TEXT)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FAV_SONGS_TABLE);
    }

    public Cursor getAllFavSongs() {
        database = this.getWritableDatabase();
        cursor = database.rawQuery("SELECT * FROM " + FAV_SONGS_TABLE, null);
        return cursor;
    }

    public boolean addFavSong(Audio song, int position) {
        database = this.getWritableDatabase();
        contentValues = new ContentValues();

        contentValues.put("INDEX_ID", position);
        contentValues.put("DATA", song.getData());
        contentValues.put("TITLE", song.getTitle());
        contentValues.put("ARTIST", song.getArtist());
        contentValues.put("DURATION", song.getDuration());

        long result = database.insert(FAV_SONGS_TABLE, null, contentValues);
        return result != -1;
    }

    public int deleteFavSong(String data) {
        database = this.getWritableDatabase();
        return database.delete(FAV_SONGS_TABLE, "DATA=?", new String[]{data});
    }
}
