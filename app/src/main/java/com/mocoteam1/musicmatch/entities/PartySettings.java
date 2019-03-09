package com.mocoteam1.musicmatch.entities;

/**
 * Created by Tristan on 13.12.2017.
 */

public class PartySettings {

    private boolean addAllowed;
    private int radius;

    public boolean isAddAllowed() {
        return addAllowed;
    }

    public void setAddAllowed(boolean addAllowed) {
        this.addAllowed = addAllowed;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }
}
