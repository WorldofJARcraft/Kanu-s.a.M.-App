package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class StartActivity extends AppCompatActivity {
    /**
     * Name der Preferences
     */
    private String PREFS_NAME = "Startzeiten";
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
        setContentView(R.layout.activity_start);
        if (isNetworkAvailable()) {
            //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
            HTTP_Connection conn = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Startnummern.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            conn.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output, long durationMillis, String url) {
                    if (spinnerleer) {
                        spinnerleer = false;
                        System.out.println("Startnummern: " + output);
                        //Spinner adressieren
                        s = (Spinner) findViewById(R.id.startnummer_start);
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
                public void processFinish(String output, long durationMillis, String url) {
                    TextView lauf = (TextView) findViewById(R.id.lauf_start);
                    try{
                        lauf.setText("Lauf: "+(Integer.parseInt(output)+1));}
                    catch(Exception e){

                    }
                }};
            conn3.execute("params");
        }
        findViewById(R.id.starter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String request = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Startzeit_eintragen.php?startnummer="+s.getSelectedItem().toString()+"&timestamp="+(System.currentTimeMillis()+ConnectionActivity.zeitdiff);
                boolean[] erfolg = {false};
                anfragen.add(request);
                if (isNetworkAvailable()) {
                    //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                    HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                    //Ergebnis der Abfrage an diese Klasse liefern
                    System.out.println("Versuche, zu starten mit: "+request);
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output, long durationMillis, String url) {
                            //Skript gibt "Erfolg!" aus, wenn korrekt eingetragen --> dann Erfolgsmeldung ausgeben, sonst nicht
                            System.out.println("Antwort auf Startanfrage: "+output);
                            if(output.equals("Erfolg!")){
                                Toast.makeText(StartActivity.this,"Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" erfolgreich gestartet!",Toast.LENGTH_SHORT).show();
                                //... --> Anfrage von Liste streichen
                                anfragen.remove(request);
                            }
                            else{
                                Toast.makeText(StartActivity.this,"Beim Start von Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" ist ein Fehler aufgetreten!" +
                                        " Die Startzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    conn.execute("params");
                }
                else{
                    Toast.makeText(StartActivity.this,"Beim Start von Startnummer "+s.getSelectedItem().toString()+" ist ein Fehler aufgetreten!" +
                            "Die Startzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_SHORT).show();
                }
                if(s.getSelectedItemPosition()<s.getCount()-1)
                    s.setSelection(s.getSelectedItemPosition()+1);
            }
        });
        /**
         * Button, der die Anzeige aktualisiert.
         */
        Button aktualisieren = (Button) findViewById(R.id.aktualisierenStart);
        aktualisieren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Activity wird beendet und neu gestartet
                finish();
                startActivity(getIntent());
            }
        });
        syncDaemon.start();
    }

    /**
     * Speichert alle Anfragen, welche noch zu senden sind.
     */
    private final ArrayList<String> anfragen = new ArrayList<>();
    private Thread syncDaemon = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()){
            for(final String request : anfragen){
                //Versuchen, die Anfrage zu versenden
                if (isNetworkAvailable()) {
                    //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                    HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE,1);
                    //Ergebnis der Abfrage an diese Klasse liefern
                    System.out.println("Versuche, zu starten mit: "+request);
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output, long durationMillis, String url) {
                            //Skript gibt "Erfolg!" aus, wenn korrekt eingetragen --> dann Erfolgsmeldung ausgeben, sonst nicht
                            System.out.println("Antwort auf Startanfrageim Daemon: "+output);
                            if(output.equals("Erfolg!")){
                                Toast.makeText(StartActivity.this,"Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&timestamp"))+" wurde mit gespeicherter Zeit erfolgreich gestartet!",Toast.LENGTH_SHORT).show();
                                //... --> Anfrage von Liste streichen
                               if(anfragen.contains(url))
                                anfragen.remove(url);
                            }
                        }
                    };
                    conn.execute("params");
                }
            }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });
    /**
     * Wird beim Beenden des Activitys aufgerufen
     */
    @Override
    public void onDestroy()
    {
        //Vernichtung
        super.onDestroy();
        //Abbruch des Hintergrundthreads
        syncDaemon.interrupt();
    }
}

