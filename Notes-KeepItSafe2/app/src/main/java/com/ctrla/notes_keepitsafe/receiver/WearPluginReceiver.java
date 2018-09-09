
 
package com.ctrla.notes_keepitsafe.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.FileOutputStream;
import java.io.IOException;

public class WearPluginReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Write note to disk
            FileOutputStream output = context.openFileOutput(String.valueOf(System.currentTimeMillis()), Context.MODE_PRIVATE);
            output.write(intent.getByteArrayExtra("note"));
            output.close();
        } catch (IOException e) { /* Gracefully fail */ }

        // Send broadcast to NoteListFragment to refresh list of notes
        Intent listNotesIntent = new Intent();
        listNotesIntent.setAction("com.farmerbb.notepad.LIST_NOTES");
        LocalBroadcastManager.getInstance(context).sendBroadcast(listNotesIntent);
    }
}
