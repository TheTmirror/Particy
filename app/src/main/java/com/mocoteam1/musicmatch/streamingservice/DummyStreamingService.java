package com.mocoteam1.musicmatch.streamingservice;

import android.util.Log;

import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.entities.StreamingServiceSong;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tristan on 04.01.2018.
 */

public class DummyStreamingService implements IStreamingService {

    private static final String DEBUG_TAG = DummyStreamingService.class.getSimpleName();

    @Override
    public void playSong(String uri) {
        Log.d(DEBUG_TAG, "Song wurde abgespielt");
    }

    @Override
    public List<StreamingServicePlaylist> getPlaylists() {
        List<StreamingServicePlaylist> list = new LinkedList<StreamingServicePlaylist>();
        for(int i = 0; i < 10; i++)
            list.add(new StreamingServicePlaylist("Dummy Name " + i));
        return list;
    }

    @Override
    public List<Song> getSong(String uri) {
        List<Song> song = new LinkedList<Song>();
        return song;
    }

    @Override
    public boolean removePermissions() {
        return true;
    }

    @Override
    public boolean login(int requestCode) {
        Log.d(DEBUG_TAG, "User logged in");
        return true;
    }

    @Override
    public void onDestroy() {

    }

}
