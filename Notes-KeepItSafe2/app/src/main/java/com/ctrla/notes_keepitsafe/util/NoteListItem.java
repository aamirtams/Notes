

package com.ctrla.notes_keepitsafe.util;

import java.text.Collator;
import java.util.Comparator;

public class NoteListItem {
    private String note;
    private String date;

    public NoteListItem(String note, String date) {
        this.note = note;
        this.date = date;
    }

    public String getNote() {
      return note;
    }

    public String getDate() {
        return date;
    }

    public static Comparator<NoteListItem> NoteComparatorTitle = new Comparator<NoteListItem>() {
        @Override
        public int compare(NoteListItem arg1, NoteListItem arg2) {
            return Collator.getInstance().compare(arg1.getNote(), arg2.getNote());
        }
    };
}
