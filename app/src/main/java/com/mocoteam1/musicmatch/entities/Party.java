package com.mocoteam1.musicmatch.entities;

import java.util.HashMap;

/**
 * Created by Tristan on 09.12.2017.
 */

public class Party {

    private int partyID;
    private String partyName;
    private String partyOwner;
    private double length;
    private double width;
    private PartySettings settings;
    private HashMap<Integer, Song> playlist;

    public String getPartyOwner() {
        return partyOwner;
    }

    public void setPartyOwner(String partyOwner) {
        this.partyOwner = partyOwner;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public int getPartyID() {
        return partyID;
    }

    public void setPartyID(int partyID) {
        this.partyID = partyID;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public PartySettings getSettings() {
        return settings;
    }

    public void setSettings(PartySettings settings) {
        this.settings = settings;
    }

    public HashMap<Integer, Song> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(HashMap<Integer, Song> playlist) {
        this.playlist = playlist;
    }
}
