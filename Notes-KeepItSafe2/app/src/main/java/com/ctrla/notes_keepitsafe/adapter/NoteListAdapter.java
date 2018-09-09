
package com.ctrla.notes_keepitsafe.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ctrla.notes_keepitsafe.R;
import com.ctrla.notes_keepitsafe.util.NoteListItem;

import java.util.ArrayList;

public class NoteListAdapter extends ArrayAdapter<NoteListItem> {
    public NoteListAdapter(Context context, ArrayList<NoteListItem> notes) {
        super(context, R.layout.row_layout, notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        NoteListItem item = getItem(position);
        String note = item.getNote();

        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout, parent, false);

        // Lookup view for data population
        TextView noteTitle = (TextView) convertView.findViewById(R.id.noteTitle);

        // Populate the data into the template view using the data object
        noteTitle.setText(note);

        // Apply theme
        SharedPreferences pref = getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String theme = pref.getString("theme", "light-sans");

        if(theme.contains("light"))
            noteTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_primary));

        if(theme.contains("dark"))
            noteTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_primary_dark));

        if(theme.contains("sans"))
            noteTitle.setTypeface(Typeface.SANS_SERIF);

        if(theme.contains("serif"))
            noteTitle.setTypeface(Typeface.SERIF);

        if(theme.contains("monospace"))
            noteTitle.setTypeface(Typeface.MONOSPACE);

        switch(pref.getString("font_size", "normal")) {
            case "smallest":
                noteTitle.setTextSize(12);
                break;
            case "small":
                noteTitle.setTextSize(14);
                break;
            case "normal":
                noteTitle.setTextSize(16);
                break;
            case "large":
                noteTitle.setTextSize(18);
                break;
            case "largest":
                noteTitle.setTextSize(20);
                break;
        }

        // Return the completed view to render on screen
        return convertView;
    }
}