package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Exchanger;

/*
Quellen:
    http://stackoverflow.com/questions/3732790/android-split-string
    http://stackoverflow.com/questions/16686298/string-timestamp-to-calendar-in-java
    http://stackoverflow.com/questions/8505707/android-best-and-safe-way-to-stop-thread
 */

/**
 * Activity, in dem die Zeit für einen Teilnehmer beim Erreichen einer Messstation genommen wird
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Context context;
    /**
     * Speichert, ob der Spinner, welche die Tore anzeigt, schon eingetragene Werte hat
     */
    private boolean spinner2leer = true;
    /**
     * verfügbare Tore dieser Messstation
     */
    private String[] arraySpinner2;

    private Spinner[] strafen;
    /**
     * Name der Preferences
     */
    private String PREFS_NAME = "Strafen";
    private TextView[] tore;

    /**
     * Spielt ein kurzes Signal ab, wenn der Startbefehl eingegeben wurde.
     */
    private MediaPlayer player;
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
     * Wird beim Start des Activitys ausgeführt
     *
     * @param savedInstanceState Zustand der Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //onCreate-Void der Superklasse ausführen
        super.onCreate(savedInstanceState);
        //Layout der Activity laden
        setContentView(R.layout.activity_main);
        letzterEintrag =  (TextView) findViewById(R.id.letzter_Eintrag);
        //Bildschirm an behalten
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //anzeigen eines Buttons "Zurück"
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setHomeAsUpIndicator(R.mipmap.ic_arrow_back_white_24dp);
        actionBar.setDisplayShowHomeEnabled(true);
        player = MediaPlayer.create(this, R.raw.okay);
        player.setVolume(1f,1f);
        //gestartet-Attribut übernehmen
        this.gestartet = ConnectionActivity.gestartet;
        context = this.getApplicationContext();
        //vergangene Zeit als Differenz zwischen Systemzeit und Startzeit berechnen --> Systemzeit muss dafür korrekt eingestellt sein!
        this.aktZeit = System.currentTimeMillis() - ConnectionActivity.startzeit;
        //Textausgabewerkzeug adressieren ...
        zeit = (TextView) findViewById(R.id.stoppuhr);
        //... und anzeigen
        zeit.setVisibility(View.VISIBLE);
        //Thread für die Stoppuhr starten
        this.uhr.start();
        //prüfen, ob Netzwerk verfügbar ist
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
                        s = (Spinner) findViewById(R.id.startnummer);
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

                        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                if(tore!=null)
                                for(final TextView tor : tore){
                                    final String request = "http://"+ConnectionActivity.IP_ADRESSE+"/AndroidConnectorAppHTTPScripts/Abfrage_Strafzeiten.php?tor="+(Integer.parseInt((String)tor.getText())-1)+"&startnummer="+s.getSelectedItem().toString();
                                    HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE, 1);
                                    conn.delegate = new AsyncResponse() {
                                        @Override
                                        public void processFinish(String output, long durationMillis, String url) {
                                            if(output!=null) {
                                                if (output.equals("50")) {
                                                    strafen[getTor("" + (Integer.parseInt(url.substring(url.indexOf("?tor=") + 5, url.indexOf("startnummer") - 1)) + 1))].setSelection(2);
                                                } else if (output.equals("2"))
                                                    strafen[getTor("" + (Integer.parseInt(url.substring(url.indexOf("?tor=") + 5, url.indexOf("startnummer") - 1)) + 1))].setSelection(1);
                                                else
                                                    strafen[getTor("" + (Integer.parseInt(url.substring(url.indexOf("?tor=") + 5, url.indexOf("startnummer") - 1)) + 1))].setSelection(0);
                                            }
                                            else{
                                                strafen[getTor("" + (Integer.parseInt(url.substring(url.indexOf("?tor=") + 5, url.indexOf("startnummer") - 1)) + 1))].setSelection(0);
                                            }

                                        }
                                    };
                                    conn.execute("params");
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });
                        //s.setSelection(0);
                        /*View v = s.getSelectedView();
                        ((TextView) v).setTextColor(Color.BLACK);*/
                        //Startnummern ermittelt --> von nun an keine PHP-Ausgaben mehr zu verarbeiten, da von hier an nur noch Eintragungen in die Datenbank
                        initialisieren = false;
                        //zuerst prüfen, ob schon Werte eingetragen werden können
                    }
                }
            };
            //Abfrage ausführen
            conn.execute("params");
            //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Tore" ausführt und so alle Tore der Messstation aus der Datenbank ausliest
            HTTP_Connection conn2 = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Tore.php?station=" + (ConnectionActivity.gewählteStation), true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            conn2.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output, long durationMillis, String url) {
                    if (spinner2leer) {
                        spinner2leer = false;
                        System.out.println("Tore: " + output);
                        //Startnummern werden im Format   Nummer|Nummer|Nummer   zurückgegeben --> einzelne Nummern ermitteln, indem output am trennzeichen "|" getrennt wird
                        //--> es entsteht ein Array mit allen verfügbaren Startnummern
                        arraySpinner2 = output.split("\\|");
                        strafen = new Spinner[arraySpinner2.length];
                        tore = new TextView[arraySpinner2.length];
                        //GUI-Tabelle adressieren
                        TableLayout strafen = (TableLayout) findViewById(R.id.strafenTabelle);
                        //neue Tabellenzeile anlegen --> Tabellenkopf
                        TableRow tbrow = new TableRow(MainActivity.this);
                        TextView tv = new TextView(MainActivity.this);
                        //TextViews anlegen, welche die Beschriftungen von Tor und Strafsekunden enthalten...
                        tv.setText("Tor: ");
                        tv.setTextColor(Color.BLACK);
                        tbrow.addView(tv);
                        TextView tvs = new TextView(MainActivity.this);
                        tvs.setText("Strafsekunden: ");
                        tvs.setTextColor(Color.BLACK);
                        //... diese der Tabellenzeile hinzufügen...
                        tbrow.addView(tvs);
                        //... und die Zeile der Tabelle hinzufügen
                        strafen.addView(tbrow);
                        //alle Tore durchgehen...
                        for (int i = 0; i < arraySpinner2.length; i++) {
                            //... diese zur Kontrolle auf Konsole ausgeben...
                            System.out.println("Tor " + (i + 1) + ": " + arraySpinner2[i]);
                            //... diese im Array speichern...
                            arraySpinner2[i] = "" + (Integer.parseInt(arraySpinner2[i]) + 1);
                            //neue Tabellenzeile anlegen...
                            TableRow tbrow0 = new TableRow(MainActivity.this);
                            //... in dieser die Nummer des Tores anzeigen...
                            TextView tv0 = new TextView(MainActivity.this);
                            tv0.setText("" + arraySpinner2[i]);
                            tv0.setTextColor(Color.BLACK);
                            tbrow0.addView(tv0);
                                /*TextView tv1 = new TextView(MainActivity.this);
                                tv1.setText("0");
                                tv1.setTextColor(Color.WHITE);
                                tbrow0.addView(tv1);*/
                            //... und einen Spinner, der die Auswahl der Strafsekunden ermöglicht
                            Spinner sp;
                            //Prüfen, ob mindestens Android 3 vorliegt
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                                //wenn ja: besser bedienbaren Spinner im Dialog-Mode anzeigen,
                                sp = new Spinner(MainActivity.this, Spinner.MODE_DIALOG);
                            } else {
                                //sonst: Standard-Spinner
                                sp = new Spinner(MainActivity.this);
                            }
                            //Array aller auswählbaren Strafwerte
                            String[] strafwerte = {"0", "2", "50"};
                            //Spinner zusammensetzen...
                            sp.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.simple_spinner_item, strafwerte));
                            //... der Tabellenzeile hinzufügen...
                            tbrow0.addView(sp);
                            MainActivity.this.strafen[i] = sp;
                            MainActivity.this.tore[i]=tv0;
                            //... und diese wiederum der Tabellenzeile hinzufügen
                            strafen.addView(tbrow0);
                        }
                        //"adapter" mit den aktuellen Startnummern neu initialisieren
                            /*adapter2 = new ArrayAdapter<String>(context,
                                    R.layout.simple_spinner_item, arraySpinner2);
                            adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item);
                            //Spinner die Werte zuordnen
                            s2.setAdapter(adapter2);
                            //niedrigste Startnummer auswählen
                            s2.setSelection(0);
                            //schwarze Items
                            s2.setSelection(0, true);
                            View v = s.getSelectedView();
                            ((TextView)v).setTextColor(Color.BLACK);*/
                        //Startnummern ermittelt --> von nun an keine PHP-Ausgaben mehr zu verarbeiten, da von hier an nur noch Eintragungen in die Datenbank
                        initialisieren = false;
                        //zuerst prüfen, ob schon Werte eingetragen werden können
                    }
                }
            };
            //Abfrage ausführen
            conn2.execute("params");
            HTTP_Connection conn3 = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Lauf.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
            //Ergebnis der Abfrage an diese Klasse liefern
            conn3.delegate = new AsyncResponse() {
                @Override
                public void processFinish(String output, long durationMillis, String url) {
                    TextView lauf = (TextView) findViewById(R.id.lauf);
                    try{
                    lauf.setText("Lauf: "+(Integer.parseInt(output)+1));}
                    catch(Exception e){

                    }
            }};
            conn3.execute("params");
            //Button ermitteln, der die Zeit eintragen soll...
            Button eintragen = (Button) findViewById(R.id.zeitnahme);
            //... und ihm die aktuelle Klasse als OnClickListener zuweisen
            eintragen.setOnClickListener(this);
            //TextView addressieren...
            TextView begrüßung = (TextView) findViewById(R.id.anweisung_Wahl_SN);
            //... und ihm die Nummer der aktuellen Messstation hinzufügen, die zuvor aus der ConnectionActivity ausgelesen wird
            begrüßung.setText(" Eingeloggt als Messstation: " + (ConnectionActivity.gewählteStation + 1) + ".");
        }
        //wird aufgerufen, wenn das Netzwerk nicht verfügbar ist
        else {
            //Fehlermeldung ausgeben
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden: Netzwerk nicht verfügbar!", Toast.LENGTH_LONG).show();
        }
        syncDaemon.start();
        /**
         * Button, der die Anzeige aktualisiert.
         */
        Button aktualisieren = (Button) findViewById(R.id.aktualisierenButton);
        aktualisieren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Activity wird beendet und neu gestartet
                finish();
                startActivity(getIntent());
            }
        });
        //AKtion zuweisen, die beim kentern einer Startnummer durchgeführt wird
        Button gek = (Button) findViewById(R.id.gekentert);
        gek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Stoppen des Starters
                final String request = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Zielzeit_eintragen.php?startnummer="+s.getSelectedItem().toString()+"&timestamp="+(System.currentTimeMillis()+ConnectionActivity.zeitdiff);
                anfragen.add(request);
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
                                Toast.makeText(MainActivity.this,"Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" erfolgreich gestoppt!",Toast.LENGTH_SHORT).show();
                                //... --> Anfrage von Liste streichen
                                anfragen.remove(request);
                            }
                            else{
                                Toast.makeText(MainActivity.this,"Beim Stoppen von Startnummer "+url.substring(url.indexOf("startnummer=")+12, url.indexOf("&time"))+" ist ein Fehler aufgetreten!" +
                                        " Die Zielzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    conn.execute("params");
                }
                else{
                    Toast.makeText(MainActivity.this,"Beim Stoppen von Startnummer "+s.getSelectedItem().toString()+" ist ein Fehler aufgetreten!" +
                            "Die Zielzeit wurde gespeichert und wird bei einer Wiederherstellung der Verbindung automatisch übermittelt!",Toast.LENGTH_SHORT).show();
                }
                //Eintragen eines astronomisch hohen Strafwerts
                final String gewählte_Nummer = arraySpinner[s.getSelectedItemPosition()];
                //ermitteln, für welche Station der Wert eingetragen werden soll
                int Station = ConnectionActivity.gewählteStation;
                /**
                 * Speichert, ob die Anfrage angekommen ist
                 */
                final boolean[] erfolg = {false};
                //Initialisierung definitiv abgeschlossen
                initialisieren = false;
                //Tabelle adressieren...
                TableLayout layout = (TableLayout) findViewById(R.id.strafenTabelle);
                System.out.println("Tore: " + layout.getChildCount());
                //... und jede zeile einzeln durchgehen; Kopfzeile ignorieren
                for (int i = 0; i < strafen.length; i++) {
                    //Tor und Strafe initialisieren...
                    String strafe = "9999";
                    String tor = "0";
                    //... und aktuelle Zeile auslesen
                    if (tore[i] != null)
                        tor = tore[i].getText().toString();
                    //Kontrollausgabe
                    System.out.println("Eintragen von Zeit für Startnummer: " + gewählte_Nummer + " bei Station: " + Station + " in Tor: " + tor);
                    //Prüfen, dass auch ja eine korrekte Zuordnung existiert
                    if (Integer.parseInt(strafe) != -1) {
                        //Anfrage zusammensetzen
                        final String request2 = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/zeit_eintragen.php?station=" + (Integer.parseInt(tor) - 1)
                                + "&startnummer=" + (Integer.parseInt(gewählte_Nummer)) + "&strafe=" + strafe;
                        //Anfragen werden gespeichert, um später löschen zu können
                        anfragen.add(request2);
                        //"zeit_eintragen.php" aufrufen, um die aktuelle Zeit für die gewählte Startnummer zu nehmen
                        if (isNetworkAvailable()) {
                            //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                            HTTP_Connection conn = new HTTP_Connection(request2, true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                            //Ergebnis der Abfrage an diese Klasse liefern
                            System.out.println("Versuche, zu starten mit: " + request);
                            conn.delegate = new AsyncResponse() {
                                @Override
                                public void processFinish(String output, long durationMillis, String url) {
                                    //Skript gibt "Erfolg!" aus, wenn korrekt eingetragen --> dann Erfolgsmeldung ausgeben, sonst nicht
                                    System.out.println("Antwort auf Strafzeitenanfrage: " + output);
                                    if (output.equals("Erfolg")) {
                                        Toast.makeText(MainActivity.this, "Strafzeiten für Startnummer " + url.substring(url.indexOf("&startnummer=")+13,url.indexOf("&strafe")) + " erfolgreich eingetragen!", Toast.LENGTH_SHORT).show();
                                        //... --> Anfrage von Liste streichen
                                        anfragen.remove(request2);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Beim Eintragen von Strafzeiten für Startnummer " + url.substring(url.indexOf("&startnummer=")+13,url.indexOf("&strafe")) + " ist ein Fehler aufgetreten!" +
                                                " Die Strafzeiten wurden gespeichert und werden bei einer Wiederherstellung der Verbindung automatisch übermittelt!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            };
                            conn.execute("params");
                        } else {
                            Toast.makeText(MainActivity.this, "Beim Eintragen von Strafzeiten für Startnummer " + s.getSelectedItem().toString() + " ist ein Fehler aufgetreten!" +
                                    " Die Strafzeiten wurden gespeichert und werden bei einer Wiederherstellung der Verbindung automatisch übermittelt!", Toast.LENGTH_LONG).show();
                        }
                    }
            }
                //nächste Startnummer vorwählen
                if(s.getSelectedItemPosition() < s.getCount() - 1)
                    s.setSelection(s.getSelectedItemPosition()+1);
        }});
    }
    public int getTor(String tor){
        int i=0;
        for(int j=0;j<tore.length;j++){
            if(tore[j].getText().equals(tor)){
                return j;
            }
        }
        return i;
    }
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

    /**
     * Ausgabe der Stoppuhr
     */
    TextView zeit;

    /**
     * Spinner, der die Startnummern speichert, welche die Messstation noch nicht erreicht haben
     */
    Spinner s;
    /**
     * die auswählbaren Werte des Spinners
     */
    String[] arraySpinner = null;
    /**
     * speichert, ob gerade der erste Aufruf von HTTP_Connection erfolgte
     */
    boolean initialisieren = true;
    /**
     * ArrayAdapter des Spinners, der die Werte enthält
     */
    ArrayAdapter<String> adapter;
    /**
     * HTTP_Connection hatte einen Verbindungsfehler
     */
    public static boolean CONNECTION_ERROR = false;
    /**
     * HTTP_Connection hatte einen SQL-Fehler
     */
    public static boolean SQL_ERROR = false;
    /**
     * der Wettkampf ist als gestartet markiert
     */
    private boolean gestartet = false;
    /**
     * speichert, ob die Stoppuhr läuft
     */
    //private boolean running = false;
    /**
     * Wettkampfzeit in Sekunden
     */
    //private double startzeit = 0;
    /**
     * vergangene Zeit seit Wettkampfstart
     */
    private double aktZeit = 0;
    /**
     * der Code, der die Anzeige der Stoppuhr durchführt wird
     */
    private Thread uhr = new Thread() {
        /**
         * Prozess für die Synchronisation
         */
        public void run() {
            //solange nicht die Thread.Interrupt()-Methode aufgerufen wurde --> dann stoppen
            while (!Thread.currentThread().isInterrupted()) {
                //Hundertstelsekunde warten
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //jeneHundertstelsekunde zur vergangenen Zeit hinzuaddieren
                aktZeit += 10;
                //aus Timestamp Datum machen
                Date d = new Date((long) aktZeit);
                //Calendar initialisieren...
                Calendar c = Calendar.getInstance();
                //... und diesem den Timestamp als Datum übergeben
                c.setTime(d);
                //vergangene Jahre ermitteln; 1970 abziehen, da dies der Minimalwert ist
                int year = c.get(Calendar.YEAR) - 1970;
                //vergangene Monate ermitteln
                int month = c.get(Calendar.MONTH);
                //vergangene Tage ermitteln, 1 abziehen, da immer 1 zu groß
                int date = c.get(Calendar.DATE) - 1;
                //vergangene Stunden ermitteln, 1 abziehen, da imer 1 zu groß
                int hour = c.get(Calendar.HOUR_OF_DAY) - 1;
                //vergangene Minuten, Sekunden, Milisekunden ermitteln
                int minute = c.get(Calendar.MINUTE);
                int second = c.get(Calendar.SECOND);
                int millis = c.get(Calendar.MILLISECOND);
                //String für Zeitangabe initialisieren
                String zeitString = "";
                //Falls ein Wert nicht 0 ist, diesen aufführen
                if (year != 0) {
                    zeitString += year + " Jahre, ";
                }
                if (month != 0) {
                    zeitString += month + " Monate, ";
                }
                if (date != 0) {
                    zeitString += date + " Tage, ";
                }
                if (hour != 0) {
                    zeitString += hour + " Stunden, ";
                }
                if (minute != 0) {
                    zeitString += minute + " Minuten, ";
                }
                if (second != 0) {
                    zeitString += second + " Sekunden, ";
                }
                if (millis != 0) {
                    zeitString += millis + " Millisekunden. ";
                }
                //Anzeige in finalem String speichern --> damit er vom UI-Thread aus aufgerufen werden kann
                final String ausgabe = zeitString;
                //auf UI-Thread zugreifen, dort die Anzeige der Stoppuhr zuweisen --> muss so erfokgen, da Zugriff auf TextView aus anderem Thread heraus nicht möglich
                runOnUiThread(new Runnable() {
                    public void run() {
                        zeit.setText("Zeit seit Start: " + ausgabe);
                    }
                });

            }
        }
    };
    /**
     * false, sobald der Spinner mit den Startnummern keine weiteren Elemente enthält
     */
    boolean spinnerleer = true;

    /**
     * Methode, die nach einem Durchlauf von HTTP_Connection ausgeführt wird
     *
     * @param output das Resultat des Seitenaufrufs
     */
    /*
    @Override
    public void processFinish(String output) {
        //Beim ersten Aufruf von HTTP_Connection , wenn es keine Fehler gab, ...
        if (initialisieren && !(CONNECTION_ERROR || SQL_ERROR) && arraySpinner == null) {
            //... den Spinner mit allen verfügbaren Startnummern füllen

        } else {
            initialisieren = false;
        }
        //geprüft, ob Wettkampf schon gestartet wurde
        if (abfrage_Gestartet) {
            //Flagge zurücksetzen

        }
        //übrig gebliebener Code
        /*if (abfrage_Startzeit && !abfrage_Gestartet) {
            try {
                System.out.println("Startzeit: " + output);
                startzeit = Double.parseDouble(output);
            } catch (NumberFormatException e) {
            }

        }
        //eventuelle Fehler verarbeiten
        if (this.SQL_ERROR) {
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden: Es ist ein internes Problem aufgetreten! Bitte benachrichtigen Sie den Entwickler!", Toast.LENGTH_LONG).show();
        }
        if (this.CONNECTION_ERROR) {
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden: Verbindung zum Server kann nicht hergestellt werden!"
                    + " Bitte prüfen Sie, dass Sie die IP-Adresse des Servers korrekt eingegeben haben und mit dem korrekten Netzwerk verbunden sind!", Toast.LENGTH_LONG).show();
        }
    }
*/

    /**
     * speichert, ob gerade per PHP-Script abgefragt wurde, ob der Wettkampf schon gestartet ist
     */
    //private boolean abfrage_Gestartet = false;
    /**
     * speichert, ob gerade per PHP-Script die Startzeit abgefragt wurde
     */
    //private boolean abfrage_Startzeit = false;
    private final List<String> anfragen = new ArrayList<>();
    private TextView letzterEintrag;
    /**
     * Wird aufgerufen, um die aktuelle Zeit für die gewählte Startnummer einzutragen; ist den "Zeit nehmen"-Button zugeordnet.
     *
     * @param view der angeklickte Button
     */
    @Override
    public void onClick(View view) {
        /**
         * Speichert Srafnummer und ihre Strafen zur Ausgabe an den Benutzer
         */
        StringBuilder aktWerte = new StringBuilder("\nStartnummer "+arraySpinner[s.getSelectedItemPosition()]);
        player.start();
        //Vibrieren
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vib.vibrate(500);
        //wenn Wettkampf nicht gestartet...
        //falls keine Fehler aufgetreten sind und es noch Elemente im Spinner gibt...
        //if (s != null && s.getSelectedItem() != null && !(SQL_ERROR || CONNECTION_ERROR)) {
        //Startzeit ermitteln, Stoppuhr starten
        //Position des ausgewählten Items im Spinner = seine Position in ArraySpinner
        //--> ausgewählte Startnummer ist der Wert an dieser Position
        final String gewählte_Nummer = arraySpinner[s.getSelectedItemPosition()];
        //ermitteln, für welche Station der Wert eingetragen werden soll
        int Station = ConnectionActivity.gewählteStation;
        /**
         * Speichert, ob die Anfrage angekommen ist
         */
        final boolean[] erfolg = {false};
        /**
         * Speichert alle HTTP-Requests
         */
                    /*
                    //ArraySpinner in Linked´List konvertieren...
                    List<String> liste = new LinkedList<String>(Arrays.asList(arraySpinner));
                    //... um das aktuell ausgewählte Item entfernen zu können --> keine Doppelauswahl von Stratnummern, um Fehler zu vermeiden
                    liste.remove(s.getSelectedItemPosition());
                    //Rückkonvertierung in Array of String
                    arraySpinner = liste.toArray(new String[0]);
                    //übrige Startnummern wieder in Spinner eintragen
                    adapter = new ArrayAdapter<String>(this,
                            android.R.layout.simple_spinner_item, arraySpinner);
                    s.setAdapter(adapter);
                    //erste übrige Startnummer auswählen
                    s.setSelection(0);*/
        //Initialisierung definitiv abgeschlossen
        initialisieren = false;
        //Tabelle adressieren...
        TableLayout layout = (TableLayout) findViewById(R.id.strafenTabelle);
        System.out.println("Tore: " + layout.getChildCount());
        //... und jede zeile einzeln durchgehen; Kopfzeile ignorieren
        for (int i = 0; i < strafen.length; i++) {
            //Tor und Strafe initialisieren...
            String strafe = "-1";
            String tor = "0";
            //... und aktuelle Zeile auslesen
            if (tore[i] != null)
                tor = tore[i].getText().toString();
            if (strafen[i] != null)
                strafe = strafen[i].getSelectedItem().toString();
            //}


            //Kontrollausgabe
            System.out.println("Eintragen von Zeit für Startnummer: " + gewählte_Nummer + " bei Station: " + Station + " in Tor: " + tor);
            //Prüfen, dass auch ja eine korrekte Zuordnung existiert
            if (Integer.parseInt(strafe) != -1) {
                //Anfrage zusammensetzen
                aktWerte.append("\nTor: "+tor+"; Strafe: "+strafe);
                final String request = "http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/zeit_eintragen.php?station=" + (Integer.parseInt(tor) - 1)
                        + "&startnummer=" + (Integer.parseInt(gewählte_Nummer)) + "&strafe=" + strafe;
                //Anfragen werden gespeichert, um später löschen zu können
                //"zeit_eintragen.php" aufrufen, um die aktuelle Zeit für die gewählte Startnummer zu nehmen
                if (isNetworkAvailable()) {
                    //wenn ja: neue Instanz von HTTP_Connection erstellen, die das Script "Abfrage_Startnummern" ausführt und so alle Startnummern aus der Datenbank ausliest
                    HTTP_Connection conn = new HTTP_Connection(request, true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                    //Ergebnis der Abfrage an diese Klasse liefern
                    System.out.println("Versuche, zu starten mit: " + request);
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output, long durationMillis, String url) {
                            //Skript gibt "Erfolg!" aus, wenn korrekt eingetragen --> dann Erfolgsmeldung ausgeben, sonst nicht
                            System.out.println("Antwort auf Strafzeitenanfrage: " + output);
                            if (output.equals("Erfolg")) {
                                Toast.makeText(MainActivity.this, "Strafzeiten für Startnummer " + url.substring(url.indexOf("&startnummer=")+13,url.indexOf("&strafe")) + " erfolgreich eingetragen!", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(MainActivity.this, "Beim Eintragen von Strafzeiten für Startnummer " + url.substring(url.indexOf("&startnummer=")+13,url.indexOf("&strafe")) + " ist ein Fehler aufgetreten!" +
                                        " Die Strafzeiten wurden gespeichert und werden bei einer Wiederherstellung der Verbindung automatisch übermittelt!", Toast.LENGTH_LONG).show();
                                anfragen.add(request);
                            }
                        }
                    };
                    conn.execute("params");
                } else {
                    Toast.makeText(MainActivity.this, "Beim Eintragen von Strafzeiten für Startnummer " + s.getSelectedItem().toString() + " ist ein Fehler aufgetreten!" +
                            " Die Strafzeiten wurden gespeichert und werden bei einer Wiederherstellung der Verbindung automatisch übermittelt!", Toast.LENGTH_LONG).show();
                }
            }
            //}
            //wenn Bedingung eintritt, git es keine auswählbaren Elemente mehr
            /*if (s == null || s.getSelectedItem() == null) {
                //Information an den User anzeigen...
                //TextView endbotschaft = (TextView) findViewById(R.id.endbotschaft);
                //endbotschaft.setVisibility(View.VISIBLE);
                //... und Werkzeuge zum Eintragen von Zeiten ausblenden
                assert s != null;
                s.setVisibility(View.INVISIBLE);
                Button eintragen = (Button) findViewById(R.id.zeitnahme);
                eintragen.setOnClickListener(null);
                eintragen.setVisibility(View.INVISIBLE);
                TextView begrüßung = (TextView) findViewById(R.id.anweisung_Wahl_SN);
                begrüßung.setVisibility(View.INVISIBLE);
                TextView stoppuhr = (TextView) findViewById(R.id.stoppuhr);
                stoppuhr.setVisibility(View.GONE);
                //running = false;
                uhr.interrupt();
                zeit.setVisibility(View.GONE);
            }*/
            //wurde der Wettkampf noch nicht gestartet, wird eine Warnung ausgegeben

        }
        //Hinzufügen der letzten Strafwerte, damit man sieht, was man eingegeben hat
        //aktuell vorhandene Strafen hinten ansetzen --> letzte eingegebene Strafe ist immer oben
        aktWerte.append("\n").append(letzterEintrag.getText());
        //Anzeige
        letzterEintrag.setText(aktWerte.toString());

    }
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
                                System.out.println("Anfrage:"+url);
                                if(output.equals("Erfolg")){
                                    Toast.makeText(MainActivity.this,"Strafzeiten für Startnummer "+request.substring(request.indexOf("&startnummer=")+13,request.indexOf("&strafe="))+" wurden erfolgreich gespeichert!",Toast.LENGTH_SHORT).show();
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
