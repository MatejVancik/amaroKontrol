package com.mv2studio.amarok.kontrol.shared.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    private String title;
    private String artist;
    private String album;
    private int id;
    private Bitmap cover;
    private Bitmap bluredCover;
    private boolean isNewAlbum = false;
    private String lyrics;
    private int length;
    private int position;

    public Song(String title, String artist, String album){
        this.title = title;
        this.artist = artist;
        this.album = album;
    }

    public Song(String title, String artist, String album, Bitmap cover){
        this(title, artist, album);
        this.cover = cover;
    }

    public Song(int id, String title, String artist, String album) {
        this(title, artist, album);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        Song obj = (Song)o;
        if(obj == null) return false;
        return title.equals(obj.getTitle()) && artist.equals(obj.getArtist()) && album.equals(obj.getAlbum());
    }

    @Override
    public String toString() {
        return title+artist+album;
    }

    public void setNewAlbum(boolean newAlbum) {
        isNewAlbum = newAlbum;
    }

    public boolean isNewAlbum() {
        return isNewAlbum;
    }

    public boolean sameSong(Song song) {
        return title.equals(song.title) && artist.equals(song.artist);
    }

    public boolean sameSong(String title, String artist, String album) {
        return this.title.equals(title) && this.artist.equals(artist) && this.album.equals(album);
    }

    public boolean onSameAlbum(Song song){
        return song.album.equals(album) && song.artist.equals(artist);
    }

    public boolean onSameAlbum(String artist, String album) {
        return this.artist.equals(artist) && this.album.equals(album);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }

    public int getId() {
        return id;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public String getLyrics() {
        return lyrics;
    }

    public Bitmap getBlured() {
        return bluredCover;
    }

    public void setBluredCover(Bitmap blured) {
        bluredCover = blured;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    protected Song(Parcel in) {
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        id = in.readInt();
        cover = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
        bluredCover = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
        isNewAlbum = in.readByte() != 0x00;
        lyrics = in.readString();
        length = in.readInt();
        position = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeInt(id);
        dest.writeValue(cover);
        dest.writeValue(bluredCover);
        dest.writeByte((byte) (isNewAlbum ? 0x01 : 0x00));
        dest.writeString(lyrics);
        dest.writeInt(length);
        dest.writeInt(position);
    }

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };


}