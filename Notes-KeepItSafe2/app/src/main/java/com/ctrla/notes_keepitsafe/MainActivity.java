
package com.ctrla.notes_keepitsafe;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ctrla.notes_keepitsafe.fragment.NoteEditFragment;
import com.ctrla.notes_keepitsafe.fragment.NoteListFragment;
import com.ctrla.notes_keepitsafe.fragment.NoteViewFragment;
import com.ctrla.notes_keepitsafe.fragment.WelcomeFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.BackButtonDialogFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.DeleteDialogFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.FirstRunDialogFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.SaveButtonDialogFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.WearPluginDialogFragment;
import com.ctrla.notes_keepitsafe.fragment.dialog.WearPluginDialogFragmentAlt;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import us.feras.mdv.MarkdownView;

public class MainActivity extends AppCompatActivity implements
BackButtonDialogFragment.Listener, 
DeleteDialogFragment.Listener, 
SaveButtonDialogFragment.Listener,
FirstRunDialogFragment.Listener,
WearPluginDialogFragment.Listener,
NoteListFragment.Listener,
NoteEditFragment.Listener, 
NoteViewFragment.Listener {

    Object[] filesToExport;
    Object[] filesToDelete;
    int fileBeingExported;
    boolean successful = true;

    public static final int IMPORT = 42;
    public static final int EXPORT = 43;
    public static final int EXPORT_TREE = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, "ca-app-pub-5246243065157193~6993701302");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set action bar elevation
            getSupportActionBar().setElevation(getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
        }
        // Show dialog if this is the user's first time running Notepad
        SharedPreferences prefMain = getPreferences(Context.MODE_PRIVATE);
        if(prefMain.getInt("first-run", 0) == 0) {
            // Show welcome dialog
            if(getSupportFragmentManager().findFragmentByTag("firstrunfragment") == null) {
                DialogFragment firstRun = new FirstRunDialogFragment();
                firstRun.show(getSupportFragmentManager(), "firstrunfragment");
            }
        } else {
            // Check to see if Android Wear app is installed, and offer to install the Notepad Plugin
            checkForAndroidWear();

            // The following code is only present to support existing users of Notepad on Google Play
            // and can be removed if using this source code for a different app

            // Convert old preferences to new ones
            SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            if(prefMain.getInt("sort-by", -1) == 0) {
                SharedPreferences.Editor editor = pref.edit();
                SharedPreferences.Editor editorMain = prefMain.edit();

                editor.putString("sort_by", "date");
                editorMain.putInt("sort-by", -1);

                editor.apply();
                editorMain.apply();
            } else if(prefMain.getInt("sort-by", -1) == 1) {
                SharedPreferences.Editor editor = pref.edit();
                SharedPreferences.Editor editorMain = prefMain.edit();

                editor.putString("sort_by", "name");
                editorMain.putInt("sort-by", -1);

                editor.apply();
                editorMain.apply();
            }

            if(pref.getString("font_size", "null").equals("null")) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("font_size", "large");
                editor.apply();
            }


            // Rename any saved drafts from 1.3.x
            File oldDraft = new File(getFilesDir() + File.separator + "draft");
            File newDraft = new File(getFilesDir() + File.separator + String.valueOf(System.currentTimeMillis()));

            if(oldDraft.exists())
                oldDraft.renameTo(newDraft);
        }

        // Begin a new FragmentTransaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // This fragment shows NoteListFragment as a sidebar (only seen in tablet mode landscape)
        if(!(getSupportFragmentManager().findFragmentById(R.id.noteList) instanceof NoteListFragment))
            transaction.replace(R.id.noteList, new NoteListFragment(), "NoteListFragment");

        // This fragment shows NoteListFragment in the main screen area (only seen on phones and tablet mode portrait),
        // but only if it doesn't already contain NoteViewFragment or NoteEditFragment.
        // If NoteListFragment is already showing in the sidebar, use WelcomeFragment instead
        if(!((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment)
           || (getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment))) {
            if((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) == null
               && findViewById(R.id.layoutMain).getTag().equals("main-layout-large"))
               || ((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment)
               && findViewById(R.id.layoutMain).getTag().equals("main-layout-large")))
                    transaction.replace(R.id.noteViewEdit, new WelcomeFragment(), "NoteListFragment");
            else if(findViewById(R.id.layoutMain).getTag().equals("main-layout-normal"))
                transaction.replace(R.id.noteViewEdit, new NoteListFragment(), "NoteListFragment");
        }

        // Commit fragment transaction
        transaction.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkForAndroidWear();
    }

    private void checkForAndroidWear() {
        // Notepad Plugin for Android Wear sends intent with "plugin_install_complete" extra,
        // in order to verify that the main Notepad app is installed correctly
        if(getIntent().hasExtra("plugin_install_complete")) {
            if(getSupportFragmentManager().findFragmentByTag("WearPluginDialogFragmentAlt") == null) {
                DialogFragment wearDialog = new WearPluginDialogFragmentAlt();
                wearDialog.show(getSupportFragmentManager(), "WearPluginDialogFragmentAlt");
            }

            SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("show_wear_dialog", false);
            editor.apply();
        } else {
            boolean hasAndroidWear = false;

            try {
                getPackageManager().getPackageInfo("com.google.android.wearable.app", 0);
                hasAndroidWear = true;
            } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }

            if(hasAndroidWear) {
                try {
                    getPackageManager().getPackageInfo("com.farmerbb.notepad.wear", 0);
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(ComponentName.unflattenFromString("com.farmerbb.notepad.wear/com.farmerbb.notepad.wear.MobileMainActivity"));
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (PackageManager.NameNotFoundException e) {
                    SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
                    if(pref.getBoolean("show_wear_dialog", true)
                            && getSupportFragmentManager().findFragmentByTag("WearPluginDialogFragment") == null) {
                        DialogFragment wearDialog = new WearPluginDialogFragment();
                        wearDialog.show(getSupportFragmentManager(), "WearPluginDialogFragment");
                    }
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
            }
        }
    }

    // Keyboard shortcuts
    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        super.dispatchKeyShortcutEvent(event);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
                NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
                NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
                NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
                WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            }

            return true;
        }
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public void onDeleteDialogPositiveClick() {
        if(filesToDelete != null) {
            reallyDeleteNote();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            fragment.onDeleteDialogPositiveClick();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            fragment.onDeleteDialogPositiveClick();
        }
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.onBackPressed();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            fragment.onBackPressed();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            fragment.onBackPressed(null);
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.onBackPressed();
        }
    }

    @Override
    public void viewNote(String filename) {
        viewEditNote(filename, false);
    }

    @Override
    public void editNote(String filename) {
        viewEditNote(filename, true);
    }

    // Method used by selecting a existing note from the ListView in NoteViewFragment or NoteEditFragment
    // We need this method in MainActivity because sometimes getSupportFragmentManager() is null
    public void viewEditNote(String filename, boolean isEdit) {
        String currentFilename;

        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            currentFilename = fragment.getFilename();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            currentFilename = fragment.getFilename();
        } else
            currentFilename = " ";

        if(!currentFilename.equals(filename)) {
            if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
                NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
                fragment.switchNotes(filename);
            } else {
                Bundle bundle = new Bundle();
                bundle.putString("filename", filename);

                Fragment fragment;
                String tag;

                if(isEdit) {
                    fragment = new NoteEditFragment();
                    tag = "NoteEditFragment";
                } else {
                    fragment = new NoteViewFragment();
                    tag = "NoteViewFragment";
                }

                fragment.setArguments(bundle);

                // Add NoteViewFragment or NoteEditFragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.noteViewEdit, fragment, tag)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
            }
        }
    }

    @Override
    public void onBackDialogNegativeClick(String filename) {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onBackDialogNegativeClick(filename);
    }

    @Override
    public void onBackDialogPositiveClick(String filename) {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onBackDialogPositiveClick(filename);
    }

    @Override
    public void onSaveDialogNegativeClick() {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onSaveDialogNegativeClick();
    }

    @Override
    public void onSaveDialogPositiveClick() {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onSaveDialogPositiveClick();
    }

    @Override
    public void showBackButtonDialog(String filename) {
        Bundle bundle = new Bundle();
        bundle.putString("filename", filename);

        DialogFragment backFragment = new BackButtonDialogFragment();
        backFragment.setArguments(bundle);
        backFragment.show(getSupportFragmentManager(), "back");
    }

    @Override
    public void showDeleteDialog() {
        showDeleteDialog(true);
    }

    private void showDeleteDialog(boolean clearFilesToDelete) {
        if(clearFilesToDelete) filesToDelete = null;

        Bundle bundle = new Bundle();
        bundle.putInt("dialog_title",
                filesToDelete == null || filesToDelete.length == 1
                ? R.string.dialog_delete_button_title
                : R.string.dialog_delete_button_title_plural);

        DialogFragment deleteFragment = new DeleteDialogFragment();
        deleteFragment.setArguments(bundle);
        deleteFragment.show(getSupportFragmentManager(), "delete");
    }

    @Override
    public void showSaveButtonDialog() {
        DialogFragment saveFragment = new SaveButtonDialogFragment();
        saveFragment.show(getSupportFragmentManager(), "save");
    }

    @Override
    public boolean isShareIntent() {
        return false;
    }

    @Override
    public String getCabString(int size) {
        if(size == 1)
            return getResources().getString(R.string.cab_note_selected);
        else
            return getResources().getString(R.string.cab_notes_selected);
    }

    @Override
    public void deleteNote(Object[] filesToDelete) {
        this.filesToDelete = filesToDelete;
        showDeleteDialog(false);
    }

    private void reallyDeleteNote() {
        // Build the pathname to delete each file, them perform delete operation
        for(Object file : filesToDelete) {
            File fileToDelete = new File(getFilesDir() + File.separator + file);
            fileToDelete.delete();
        }

        String[] filesToDelete2 = new String[filesToDelete.length];
        Arrays.asList(filesToDelete).toArray(filesToDelete2);

        // Send broadcasts to update UI
        Intent deleteIntent = new Intent();
        deleteIntent.setAction("com.farmerbb.notepad.DELETE_NOTES");
        deleteIntent.putExtra("files", filesToDelete2);
        LocalBroadcastManager.getInstance(this).sendBroadcast(deleteIntent);

        Intent listIntent = new Intent();
        listIntent.setAction("com.farmerbb.notepad.LIST_NOTES");
        LocalBroadcastManager.getInstance(this).sendBroadcast(listIntent);

        // Show toast notification
        if(filesToDelete.length == 1)
            showToast(R.string.note_deleted);
        else
            showToast(R.string.notes_deleted);

        filesToDelete = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void exportNote(Object[] filesToExport) {
        this.filesToExport = filesToExport;

        if(filesToExport.length == 1 || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            fileBeingExported = 0;
            reallyExportNote();
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            try {
                startActivityForResult(intent, EXPORT_TREE);
            } catch (ActivityNotFoundException e) {
                showToast(R.string.error_exporting_notes);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void reallyExportNote() {
        String filename = "";

        try {
            filename = loadNoteTitle(filesToExport[fileBeingExported].toString());
        } catch (IOException e) { /* Gracefully fail */ }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, generateFilename(filename));

        try {
            startActivityForResult(intent, EXPORT);
        } catch (ActivityNotFoundException e) {
            showToast(R.string.error_exporting_notes);
        }
    }

    private String generateFilename(String filename) {
        // Remove any invalid characters
        final String[] characters = new String[]{"<", ">", ":", "\"", "/", "\\\\", "\\|", "\\?", "\\*"};

        for(String character : characters) {
            filename = filename.replaceAll(character, "");
        }

        // To ensure that the generated filename fits within filesystem limitations,
        // truncate the filename to ~245 characters.
        if(filename.length() > 245)
            filename = filename.substring(0, 245);

        return filename + ".txt";
    }

    // Methods used to generate toast notifications
    private void showToast(int message) {
        Toast toast = Toast.makeText(this, getResources().getString(message), Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showToastLong(int message) {
        Toast toast = Toast.makeText(this, getResources().getString(message), Toast.LENGTH_LONG);
        toast.show();
    }

    // Loads note from /data/data/com.farmerbb.notepad/files
    public String loadNote(String filename) throws IOException {

        // Initialize StringBuilder which will contain note
        StringBuilder note = new StringBuilder("");

        // Open the file on disk
        FileInputStream input = openFileInput(filename);
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader buffer = new BufferedReader(reader);

        // Load the file
        String line = buffer.readLine();
        while (line != null ) {
            note.append(line);
            line = buffer.readLine();
            if(line != null)
                note.append("\n");
        }

        // Close file on disk
        reader.close();

        return(note.toString());
    }

    // Loads first line of a note for display in the ListView
    @Override
    public String loadNoteTitle(String filename) throws IOException {
        // Open the file on disk
        FileInputStream input = openFileInput(filename);
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader buffer = new BufferedReader(reader);

        // Load the file
        String line = buffer.readLine();

        // Close file on disk
        reader.close();

        return(line);
    }

    // Calculates last modified date/time of a note for display in the ListView
    @Override
    public String loadNoteDate(String filename) throws IOException {
        Date lastModified = new Date(Long.parseLong(filename));
        return(DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(lastModified));
    }

    @Override
    public void showFab() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.showFab();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.showFab();
        }
    }

    @Override
    public void hideFab() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.hideFab();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.hideFab();
        }
    }

    @Override
    public void onFirstRunDialogPositiveClick() {
        // Set some initial preferences
        SharedPreferences prefMain = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefMain.edit();
        editor.putInt("first-run", 1);
        editor.apply();

        SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor2 = pref.edit();
        editor2.putBoolean("show_dialogs", false);
        editor2.apply();

        checkForAndroidWear();
    }

    @Override
    public void onWearDialogPositiveClick() {
        // Intent to Google Play
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.farmerbb.notepad.wear"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
            showToastLong(R.string.wear_two_point_oh);
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }

    @Override
    public void onWearDialogNegativeClick() {
        SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("show_wear_dialog", false);
        editor.apply();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(resultCode == RESULT_OK && resultData != null) {
            successful = true;

            if(requestCode == IMPORT) {
                Uri uri = resultData.getData();
                ClipData clipData = resultData.getClipData();

                if(uri != null)
                    successful = importNote(uri);
                else if(clipData != null)
                    for(int i = 0; i < clipData.getItemCount(); i++) {
                        successful = importNote(clipData.getItemAt(i).getUri());
                    }

                // Show toast notification
                showToast(successful
                        ? (uri == null ? R.string.notes_imported_successfully : R.string.note_imported_successfully)
                        : R.string.error_importing_notes);

                // Send broadcast to NoteListFragment to refresh list of notes
                Intent listNotesIntent = new Intent();
                listNotesIntent.setAction("com.farmerbb.notepad.LIST_NOTES");
                LocalBroadcastManager.getInstance(this).sendBroadcast(listNotesIntent);
            } else if(requestCode == EXPORT) {
                try {
                    saveExportedNote(loadNote(filesToExport[fileBeingExported].toString()), resultData.getData());
                } catch (IOException e) {
                    successful = false;
                }

                fileBeingExported++;
                if(fileBeingExported < filesToExport.length)
                    reallyExportNote();
                else
                    showToast(successful
                            ? (fileBeingExported == 1 ? R.string.note_exported_to : R.string.notes_exported_to)
                            : R.string.error_exporting_notes);

                File fileToDelete = new File(getFilesDir() + File.separator + "exported_note");
                fileToDelete.delete();
            } else if(requestCode == EXPORT_TREE) {
                DocumentFile tree = DocumentFile.fromTreeUri(this, resultData.getData());

                for(Object exportFilename : filesToExport) {
                    try {
                        DocumentFile file = tree.createFile(
                                "text/plain",
                                generateFilename(loadNoteTitle(exportFilename.toString())));
                        saveExportedNote(loadNote(exportFilename.toString()), file.getUri());
                    } catch (IOException e) {
                        successful = false;
                    }
                }

                showToast(successful ? R.string.notes_exported_to : R.string.error_exporting_notes);
            }
        }
    }

    private void saveExportedNote(String note, Uri uri) throws IOException {
        // Convert line separators to Windows format
        note = note.replaceAll("\r\n", "\n");
        note = note.replaceAll("\n", "\r\n");

        // Write file to external storage
        OutputStream os = getContentResolver().openOutputStream(uri);
        os.write(note.getBytes());
        os.close();
    }

    private boolean importNote(Uri uri) {
        try {
            File importedFile = new File(getFilesDir(), Long.toString(System.currentTimeMillis()));
            long suffix = 0;

            // Handle cases where a note may have a duplicate title
            while(importedFile.exists()) {
                suffix++;
                importedFile = new File(getFilesDir(), Long.toString(System.currentTimeMillis() + suffix));
            }

            InputStream is = getContentResolver().openInputStream(uri);
            byte[] data = new byte[is.available()];

            if(data.length > 0) {
                OutputStream os = new FileOutputStream(importedFile);
                is.read(data);
                os.write(data);
                is.close();
                os.close();
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void printNote(String contentToPrint) {
        SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        // Create a WebView object specifically for printing
        boolean generateHtml = !(pref.getBoolean("markdown", false)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        WebView webView = generateHtml ? new WebView(this) : new MarkdownView(this);

        // Apply theme
        String theme = pref.getString("theme", "light-sans");
        int textSize = -1;

        String fontFamily = null;

        if(theme.contains("sans")) {
            fontFamily = "sans-serif";
        }

        if(theme.contains("serif")) {
            fontFamily = "serif";
        }

        if(theme.contains("monospace")) {
            fontFamily = "monospace";
        }

        switch(pref.getString("font_size", "normal")) {
            case "smallest":
                textSize = 12;
                break;
            case "small":
                textSize = 14;
                break;
            case "normal":
                textSize = 16;
                break;
            case "large":
                textSize = 18;
                break;
            case "largest":
                textSize = 20;
                break;
        }

        String topBottom = " " + Float.toString(getResources().getDimension(R.dimen.padding_top_bottom_print) / getResources().getDisplayMetrics().density) + "px";
        String leftRight = " " + Float.toString(getResources().getDimension(R.dimen.padding_left_right_print) / getResources().getDisplayMetrics().density) + "px";
        String fontSize = " " + Integer.toString(textSize) + "px";

        final String css =
                "body { " +
                        "margin:" + topBottom + topBottom + leftRight + leftRight + "; " +
                        "font-family:" + fontFamily + "; " +
                        "font-size:" + fontSize + "; " +
                        "}";

        final String js =
                "var styleNode = document.createElement('style');\n" +
                        "styleNode.type = \"text/css\";\n" +
                        "var styleText = document.createTextNode('" + css + "');\n" +
                        "styleNode.appendChild(styleText);\n" +
                        "document.getElementsByTagName('head')[0].appendChild(styleNode);\n";

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(false);
        webView.setWebViewClient(new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                view.evaluateJavascript(js, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        createWebPrintJob(view);
                    }
                });
            }
        });

        // Load content into WebView
        if(generateHtml) {
            webView.loadDataWithBaseURL(null,
                    "<html><body><p>"
                            + StringUtils.replace(contentToPrint, "\n", "<br>")
                            + "</p></body></html>",
                    "text/HTML", "UTF-8", null);
        } else
            ((MarkdownView) webView).loadMarkdown(contentToPrint);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createWebPrintJob(WebView webView) {
        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);

        // Get a print adapter instance
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();

        // Create a print job with name and adapter instance
        String jobName = getString(R.string.app_name) + " Document";
        printManager.print(jobName, printAdapter,
                new PrintAttributes.Builder().build());
    }
}
