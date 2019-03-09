package com.mocoteam1.musicmatch.helper;

import com.mocoteam1.musicmatch.entities.Song;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Tristan on 16.01.2018.
 */

public class SortedLinkedList extends LinkedList {

    public boolean addSorted(Song song) {
        ListIterator<Song> itr = listIterator();
        while(true) {
            if(itr.hasNext() == false) {
                itr.add(song);
                return true;
            }

            Song elementInList = itr.next();
            if(song.compareTo(elementInList) > 0) {
                itr.previous();
                itr.add(song);
                return(true);
            }
        }
    }

    public int containsID(Song song) {
        ListIterator<Song> itr = listIterator();
        int i = 0;
        while(itr.hasNext()) {
            if(itr.next().getSongID() == song.getSongID())
                return i;
            i++;
        }

        return -1;
    }
}
