package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class StoppActivity extends AppCompatActivity {
    private boolean spinnerleer = true;

    /**
     * Prüft, ob das Netzwerk verfügbar ist. Benötigt "android.permission.ACCESS_NETWORK_STATE".
     *
     * @return true für verfügbar oder false für nicht verfügbar
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    Spinner s;
    String[] arraySpinner;
    ArrayAdapter<String> adapter;
    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopp);
        if (isNetworkAvailable()) {
            //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
            HTTP_Connection conn = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Startnummern.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            conn.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    if (spinnerleer) {
                        spinnerleer = false;
                        System.out.println("Startnummern: " + output);
                        //Spinner adressieren
                        s = (Spinner) findViewById(R.id.startnummer_stopp);
                        //Startnummern werden im Format   Nummer|Nummer|Nummer   zurückgegeben --> einzelne Nummern ermitteln, indem output am trennzeichen "|" getrennt wird
                        //--> es entsteht ein Array mit allen verfügbaren Startnummern
                        arraySpinner = output.split("\\|");
                        for (int i = 0; i < arraySpinner.length; i++) {
                            System.out.println("Startnummer " + (i + 1) + ": " + arraySpinner[i]);

                        }
                        //"adapter" mit den aktuellen Startnummern neu initialisieren
                        adapter = new ArrayAdapter<>(context,
                                R.layout.simple_spinner_item, arraySpinner);
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        //Spinner die Werte zuordnen
                        s.setAdapter(adapter);
                        //niedrigste Startnummer auswählen
                        s.setSelection(0);
                        //schwarze Items
                        s.setSelection(0, true);
                        View v = s.getSelectedView();
                        ((TextView) v).setTextColor(Color.BLACK);
                        //Startnummern ermittelt --> von nun an keine PHP-Ausgaben mehr zu verarbeiten, da von hier an nur noch Eintragungen in die Datenbank
                    }
                }
            };
            //Abfrage ausführen
            conn.execute("params");
            HTTP_Connection conn3 = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Lauf.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            conn3.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    TextView lauf = (TextView) findViewById(R.id.lauf_stopp);
                    try{
                        lauf.setText("Lauf: "+(Integer.parseInt(output)+1));}
                    catch(Exception e){

                    }
                }};
            conn3.execute("params");
        }
        findViewById(R.id.stoppButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()) {
                    //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                    HTTP_Connection conn = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Zielzeit_eintragen.php?startnummer="+s.getSelectedItem().toString(), true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                    //Ergebnis der Abfrage an diese Klasse liefern
                    System.out.println("Versuche, zu starten mit: "+"http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Startzeit_eintragen.php?startnummer="+s.getSelectedItem().toString());
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                        }
                    };
                    conn.execute("params");
                }
            }
        });
    }
}
