package com.mv2studio.amarok.kontrol.shared.model;


import android.os.Parcel;
import android.os.Parcelable;

public class Artist implements Comparable<Artist>, Parcelable {



    private int id;
    private String name;
    private int albums;
    private int songs;

    public Artist(int id, String name, int songs, int albums) {
        this.id = id;
        this.name = name;
        this.albums = albums;
        this.songs = songs;
    }

    public Artist(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAlbums() {
        return albums;
    }

    public int getSongs() {
        return songs;
    }


    @Override
    public int compareTo(Artist another) {
        return getName().toUpperCase().compareTo(another.getName().toUpperCase());
    }

    @Override
    public String toString() {
        return getName().toUpperCase();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        return prime * result + ((name == null) ? 0 : name.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        Artist other = (Artist) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }


    protected Artist(Parcel in) {
        id = in.readInt();
        name = in.readString();
        albums = in.readInt();
        songs = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(albums);
        dest.writeInt(songs);
    }

    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {

        @Override
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
}