package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StoppActivity extends AppCompatActivity {
    private boolean spinnerleer = true;

    /**
     * Name der Preferences
     */
    private String PREFS_NAME = "Zielzeiten";
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
    /**
     * Aktion beim Klick auf den zurück-Button
     * @param item der Button
     * @return true oder false
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * Spielt ein kurzes Signal ab, wenn der Startbefehl eingegeben wurde.
     */
    private MediaPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopp);
        //anzeigen eines Buttons "Zurück"
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setHomeAsUpIndicator(R.mipmap.ic_arrow_back_white_24dp);
        actionBar.setDisplayShowHomeEnabled(true);
        player = MediaPlayer.create(this, R.raw.okay);
        player.setVolume(1f,1f);
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
                        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                if (adapterView != null && view != null && arraySpinner != null) {
                                    if (i == 0) {
                                        Button vor = (Button) findViewById(R.id.vorStopp);
                                        vor.setText(R.string.keine_vorige);
                                        vor.setEnabled(false);
                                    } else {
                                        Button vor = (Button) findViewById(R.id.vorStopp);
                                        vor.setText("Startnummer " + arraySpinner[i-1] + " stoppen!!");
                                        vor.setEnabled(true);
                                    }
                                    if (i == adapterView.getCount() - 1) {
                                        Button nach = (Button) findViewById(R.id.nachStopp);
                                        nach.setText(R.string.keine_folgende);
                                        nach.setEnabled(false);
                                    } else {
                                        Button nach = (Button) findViewById(R.id.nachStopp);
                                        nach.setText("Startnummer "  + arraySpinner[i+1]+ " stoppen!!");
                                        nach.setEnabled(true);
                                    }
                                    Button starter = (Button) findViewById(R.id.stoppButton);
                                    starter.setText("Startnummer " + arraySpinner[i] + " stoppen!!");
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });
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
                    TextView lauf = (TextView) findViewById(R.id.lauf_stopp);
                    try{
                        lauf.setText("Lauf: "+(Integer.parseInt(output)+1));}
                    catch(Exception e){

                    }
                }};
            conn3.execute("params");
            syncDaemon.start();
        }
        findViewById(R.id.stoppButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopp(s.getSelectedItem().toString());
                if(s.getSelectedItemPosition()<s.getCount()-1)
                    s.setSelection(s.getSelectedItemPosition()+1);
            }
        });
        findViewById(R.id.vorStopp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopp(arraySpinner[s.getSelectedItemPosition()-1]);
            }
        });

        findViewById(R.id.nachStopp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopp(arraySpinner[s.getSelectedItemPosition()+1]);
            }
        });
        /**
         * Button, der die Anzeige aktualisiert.
         */
        Button aktualisieren = (Button) findViewById(R.id.aktualisierenStopp);
        aktualisieren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Activity wird beendet und neu gestartet
                finish();
                startActivity(getIntent());
            }
        });
    }
    private void stopp(String startnummer){
        final String request = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Zielzeit_eintragen.php?startnummer="+startnummer+"&timestamp="+(System.currentTimeMillis()+ConnectionActivity.zeitdiff);
        player.start();
        if (isNetworkAvailable()) {
            //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
            HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            System.out.println("Versuche, zu stoppen mit: "+request);
            conn.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output, long durationMillis, String url) {
                    //Skript gibt "Erfolg!" aus, wenn korrekt eingetragen --> dann Erfolgsmeldung ausgeben, sonst nicht
                    System.out.println("Antwort auf Startanfrage: "+output);
                    if(output.equals("Erfolg!")){
                        Toast.makeText(StoppActivity.this,"Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" erfolgreich gestoppt!",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(StoppActivity.this,"Beim Stoppen von Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" ist ein Fehler aufgetreten!" +
                                " Die Zielzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_LONG).show();
                        anfragen.add(request);
                    }
                }
            };
            conn.execute("params");
        }
        else{
            Toast.makeText(StoppActivity.this,"Beim Stoppen von Startnummer "+s.getSelectedItem().toString()+" ist ein Fehler aufgetreten!" +
                    "Die Zielzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Speichert alle Anfragen, welche noch zu senden sind.
     */
    private final ArrayList<String> anfragen = new ArrayList<>();
    private Thread syncDaemon = new Thread(new Runnable() {
        @Override
        public synchronized void run() {
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
                                        Toast.makeText(StoppActivity.this,"Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&timestamp"))+" wurde mit gespeicherter Zeit erfolgreich gestoppt!",Toast.LENGTH_SHORT).show();
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
