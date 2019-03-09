package com.mocoteam1.musicmatch.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.StreamingServicePlaylist;
import com.mocoteam1.musicmatch.entities.StreamingServiceSong;

import java.util.List;

/**
 * Created by Tristan on 04.01.2018.
 */

public class CreatePartyPlaylistAdapter extends BaseAdapter{

    private Context context;
    private List<StreamingServicePlaylist> playlists;

    public CreatePartyPlaylistAdapter(List<StreamingServicePlaylist> playlists, Context context) {
        this.playlists = playlists;
        this.context = context;
    }

    @Override
    public int getCount() {
        return playlists.size();
    }

    @Override
    public Object getItem(int i) {
        return playlists.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.create_party_listview_item, viewGroup, false);
        }

        TextView name = view.findViewById(R.id.createPartyListviewItemName);
        name.setText(((StreamingServicePlaylist) getItem(i)).getName());

        return view;
    }

}
