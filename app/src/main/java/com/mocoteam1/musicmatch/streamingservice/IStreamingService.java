package com.mocoteam1.musicmatch.streamingservice;

import com.mocoteam1.musicmatch.entities.Song;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.entities.StreamingServiceSong;

import java.util.List;

/**
 * Created by Tristan on 04.01.2018.
 */

public interface IStreamingService {

    public static int LOGIN_REQUEST_CODE = 27353;
    public static String LOGIN_RESULT = "loginResult";

    public void playSong(String uri);

    public List<StreamingServicePlaylist> getPlaylists();

    public List<Song> getSong(String uri);

    public boolean removePermissions();

    public boolean login(int requestCode);

    public void onDestroy();

}
