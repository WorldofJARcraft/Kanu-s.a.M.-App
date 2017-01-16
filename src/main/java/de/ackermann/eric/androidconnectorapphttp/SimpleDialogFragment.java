package de.ackermann.eric.androidconnectorapphttp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;
//Quelle: http://www.tippscom.de/einfache-dialoge-in-android-erstellen/

/**
 * von http://www.tippscom.de/einfache-dialoge-in-android-erstellen übernommen. Klasse, welche einfache Dialoge erstellen kann. Wird nicht verwendet, da dafür die ausführende Activity
 * FragmentActivity erweitern müsste.
 */
public class SimpleDialogFragment extends DialogFragment {

    private String nachricht="";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String nachricht="";
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        dialogBuilder.setMessage(nachricht);
        dialogBuilder.setPositiveButton("OK!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               //Aktion
            }
        });


        return dialogBuilder.create();
    }

    public void setNachricht(String nachricht){
        this.nachricht = nachricht;

    }
}
