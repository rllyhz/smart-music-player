package id.rllyhz.mobilecomputingproject.Model;

import android.graphics.Bitmap;
import java.io.Serializable;

public class Audio implements Serializable {
    private String data;
    private String title;
    private String artist;
    private String duration;
    private String album;
    private Bitmap albumArt;
    private boolean isFavorite;

    public Audio(String data, String title, String album, Bitmap albumArt, String artist, String duration, boolean isFavorite) {
        this.data = data;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.album = album;
        this.albumArt = albumArt;
        this.isFavorite = isFavorite;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    public boolean isFavorite() {
        return this.isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }
}
