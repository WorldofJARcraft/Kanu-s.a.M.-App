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
                public void processFinish(String output, long durationMillis) {
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
                public void processFinish(String output, long durationMillis) {
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
                final String request = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Zielzeit_eintragen.php?startnummer="+s.getSelectedItem().toString()+"&timestamp="+(System.currentTimeMillis()+ConnectionActivity.zeitdiff);
                final List<String> anfragen = new ArrayList<>();
                if (isNetworkAvailable()) {
                    //Zugriff auf Speicherdaten der App
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    //Liste der abzusendenden Anfragen ermitteln...
                    String zuErledigen = settings.getString("Zielzeiten", "");
                    //... und die aktuelle Anfrage hinzufügen
                    zuErledigen+="|"+request;
                    //Anfragen werden gespeichert, um später löschen zu können
                    anfragen.add(request);
                    //Speichern des geänderten Werts
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("Zielzeiten", zuErledigen);

                    // Commit the edits!
                    editor.commit();
                    //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                    HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                    //Ergebnis der Abfrage an diese Klasse liefern
                    System.out.println("Versuche, zu starten mit: "+request);
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output, long durationMillis) {
                            //Skript gibt "Erfolg" zurück, wenn Daten eingetragen --> prüfen, ob vorliegt
                            if(output.equals("Erfolg!")){
                                //wenn ja: Erfolgsmeldung
                                Toast.makeText(StoppActivity.this,"Startnummer "+s.getSelectedItem().toString()+" erfolgreich gestoppt!",Toast.LENGTH_SHORT).show();
                                //... --> Anfrage von Liste streichen
                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                String zuErledigen = settings.getString("Zielzeiten", "");
                                zuErledigen= zuErledigen.substring(0,zuErledigen.indexOf(anfragen.get(0))-1)+zuErledigen.substring(zuErledigen.indexOf(anfragen.get(0))+anfragen.get(0).length());
                                anfragen.remove(0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("Zielzeiten", zuErledigen);
                                // Commit the edits!
                                editor.commit();

                            }
                            else{
                                //sonst: Fehlermeldung
                                Toast.makeText(StoppActivity.this,"Beim Stoppen von Startnummer "+s.getSelectedItem().toString()+" ist ein Fehler aufgetreten!",Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    conn.execute("params");
                }
                else{
                    Toast.makeText(StoppActivity.this,"Beim Stoppen von Startnummer "+s.getSelectedItem().toString()+" ist ein Fehler aufgetreten!",Toast.LENGTH_SHORT).show();
                }
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
    /**
     * Wird beim Beenden des Activitys aufgerufen
     */
    @Override
    public void onDestroy()
    {
        //Vernichtung
        super.onDestroy();
        //alle gespeicherten Anfragen löschen, damit nicht mit erneutem Start interferieren
        SharedPreferences myPrefs = this.getSharedPreferences(PREFS_NAME,0);
        myPrefs.edit().remove("Zielzeiten");
        myPrefs.edit().clear();
        myPrefs.edit().commit();
    }
}
