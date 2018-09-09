
package com.ctrla.notes_keepitsafe.fragment.dialog;

import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.ctrla.notes_keepitsafe.R;

public class WearPluginDialogFragmentAlt extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.congratulations)
        .setTitle(R.string.install_wear_plugin_title)
        .setPositiveButton(R.string.action_close, null);

        // Create the AlertDialog object and return it
        return builder.create();
    }
}