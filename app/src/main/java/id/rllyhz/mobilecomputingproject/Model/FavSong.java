package id.rllyhz.mobilecomputingproject.Model;

import java.io.Serializable;

public class FavSong implements Serializable {
    private int id;
    private int index;
    private String data;
    private String title;
    private String artist;
    private String duration;

    public FavSong(int id, int index, String data, String title, String artist, String duration) {
        this.id = id;
        this.index = index;
        this.data = data;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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
}
