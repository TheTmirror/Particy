package com.mocoteam1.musicmatch.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.Song;

import java.util.List;

/**
 * Created by Tristan on 13.01.2018.
 */

public class SearchResultAdapter extends BaseAdapter{

    List<Song> container;
    Context context;

    public SearchResultAdapter(Context context, List<Song> container) {
        this.context = context;
        this.container = container;
    }

    @Override
    public int getCount() {
        return container.size();
    }

    @Override
    public Song getItem(int i) {
        return container.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.search_sample_song_listview_item, viewGroup, false);
        }

        TextView name = (TextView) view.findViewById(R.id.searchSampleSongListviewName);
        TextView artist = (TextView) view.findViewById(R.id.searchSampleSongListviewArtist);
        TextView album = (TextView) view.findViewById(R.id.searchSampleSongListviewAlbum);

        name.setText(getItem(i).getName());
        artist.setText(getItem(i).getArtist());
        album.setText(getItem(i).getAlbum());

        return view;
    }
}
