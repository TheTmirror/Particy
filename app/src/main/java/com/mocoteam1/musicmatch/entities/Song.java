package com.mocoteam1.musicmatch.entities;

import android.support.annotation.NonNull;

/**
 * Created by Tristan on 12.12.2017.
 */

public class Song implements Comparable{


    //TODO: Überprüfen ob Tracks als Explicit gelabeld werden müssen
    private int songID;
    private String name;
    private String artist;
    private String album;
    private int upvotes;
    private int downvotes;
    private String spotifyID;
    private boolean isExplicit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void addArtist(String artist) {
        if(this.artist == null) {
            this.artist = artist;
        } else {
            this.artist += (", " + artist);
        }
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public int getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(int downvotes) {
        this.downvotes = downvotes;
    }

    public int getVotesum() {
        return upvotes - downvotes;
    }

    public int getSongID() {
        return songID;
    }

    public void setSongID(int songID) {
        this.songID = songID;
    }

    public String getSpotifyID() {
        return spotifyID;
    }

    public void setSpotifyID(String spotifyID) {
        this.spotifyID = spotifyID;
    }

    public boolean isExplicit() {
        return isExplicit;
    }

    public void setExplicit(boolean explicit) {
        isExplicit = explicit;
    }

    @Override
    public String toString() {
        return name + " " + artist + " " + getVotesum();
    }

    @Override
    public int compareTo(@NonNull Object o) {
        Song song = (Song) o;
        return getVotesum() - song.getVotesum();
    }
}
