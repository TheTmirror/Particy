package com.mocoteam1.musicmatch.helper;

/**
 * Created by Tristan on 12.12.2017.
 */

public class Math {


    //TODO: Nicht eigener Code -> Überprüfen
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

}
