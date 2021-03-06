

package com.ctrla.notes_keepitsafe.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ctrla.notes_keepitsafe.R;
import com.ctrla.notes_keepitsafe.util.NoteListItem;

import java.util.ArrayList;

public class NoteListDateAdapter extends ArrayAdapter<NoteListItem> {
    public NoteListDateAdapter(Context context, ArrayList<NoteListItem> notes) {
        super(context, R.layout.row_layout_date, notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        NoteListItem item = getItem(position);
        String note = item.getNote();
        String date = item.getDate();

        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
           convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout_date, parent, false);

        // Lookup view for data population
        TextView noteTitle = (TextView) convertView.findViewById(R.id.noteTitle);
        TextView noteDate = (TextView) convertView.findViewById(R.id.noteDate);

        // Populate the data into the template view using the data object
        noteTitle.setText(note);
        noteDate.setText(date);

        // Apply theme
        SharedPreferences pref = getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String theme = pref.getString("theme", "light-sans");

        if(theme.contains("light")) {
            noteTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_primary));
            noteDate.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_secondary));
        }

        if(theme.contains("dark")) {
            noteTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_primary_dark));
            noteDate.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_secondary_dark));
        }

        if(theme.contains("sans")) {
            noteTitle.setTypeface(Typeface.SANS_SERIF);
            noteDate.setTypeface(Typeface.SANS_SERIF);
        }

        if(theme.contains("serif")) {
            noteTitle.setTypeface(Typeface.SERIF);
            noteDate.setTypeface(Typeface.SERIF);
        }

        if(theme.contains("monospace")) {
            noteTitle.setTypeface(Typeface.MONOSPACE);
            noteDate.setTypeface(Typeface.MONOSPACE);
        }

        switch(pref.getString("font_size", "normal")) {
            case "smallest":
                noteTitle.setTextSize(12);
                noteDate.setTextSize(8);
                break;
            case "small":
                noteTitle.setTextSize(14);
                noteDate.setTextSize(10);
                break;
            case "normal":
                noteTitle.setTextSize(16);
                noteDate.setTextSize(12);
                break;
            case "large":
                noteTitle.setTextSize(18);
                noteDate.setTextSize(14);
                break;
            case "largest":
                noteTitle.setTextSize(20);
                noteDate.setTextSize(16);
                break;
        }

        // Return the completed view to render on screen
        return convertView;
    }
}