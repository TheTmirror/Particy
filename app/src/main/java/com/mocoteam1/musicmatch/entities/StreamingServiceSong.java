package com.mocoteam1.musicmatch.entities;

/**
 * Created by Tristan on 04.01.2018.
 */

public class StreamingServiceSong {

    private String name;

    public StreamingServiceSong(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
