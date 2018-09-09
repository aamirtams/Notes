
package com.ctrla.notes_keepitsafe.fragment.dialog;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.ctrla.notes_keepitsafe.R;

public class FirstRunDialogFragment extends DialogFragment {

    /* The activity that creates an instance of this fragment must
     * implement this interface in order to receive event call backs. */
    public interface Listener {
        void onFirstRunDialogPositiveClick();
    }

    // Use this instance of the interface to deliver action events
    Listener listener;

    // Override the Fragment.onAttach() method to instantiate the Listener
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the Listener so we can send events to the host
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement Listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.first_run)
        .setTitle(R.string.app_name)
        .setPositiveButton(R.string.action_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                listener.onFirstRunDialogPositiveClick();
            }
        });

        // Prevent the user from cancelling this particular dialog
        setCancelable(false);

        // Create the AlertDialog object and return it
        return builder.create();
    }
}