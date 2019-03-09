package com.mocoteam1.musicmatch.helper;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mocoteam1.musicmatch.R;
import com.mocoteam1.musicmatch.entities.Party;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Tristan on 09.12.2017.
 */

public class PartyAdapter extends BaseAdapter{

    private Context context;
    private HashMap<Integer, Party> parties;
    private String username;

    public PartyAdapter(Context context, HashMap<Integer, Party> parties, String username) {

        this.context = context;
        this.parties = parties;
        this.username = username;

    }

    @Override
    public int getCount() {
        return parties.size();
    }

    @Override
    public Party getItem(int i) {
        Iterator it = parties.entrySet().iterator();
        HashMap.Entry<Integer, Party> result = null;
        for(int j = 0; j <= i ; j++)
            result = (HashMap.Entry<Integer, Party>) it.next();
        return result.getValue();
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).getPartyID();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.party_overview_listview_item, viewGroup, false);
        }

        TextView name = (TextView) view.findViewById(R.id.partyOverviewListviewItemName);
        TextView owner = (TextView) view.findViewById(R.id.partyOverviewListviewItemOwner);

        if(getItem(i).getPartyOwner() != null && getItem(i).getPartyOwner().equals(username)) {
            ConstraintLayout hostContent = (ConstraintLayout) view.findViewById(R.id.partyOverviewListviewItemContent);
            hostContent.setBackgroundColor(context.getResources().getColor(R.color.party_overview_own_party_indicator));
        }
        name.setText(getItem(i).getPartyName());
        owner.setText(getItem(i).getPartyOwner());

        return view;
    }
}
