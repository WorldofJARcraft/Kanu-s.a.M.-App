package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;
/*
Quellen:
    http://stackoverflow.com/questions/6558364/android-development-toggling-textview-visibility
    Androidbuch, S.250
    https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html
    http://openbook.rheinwerk-verlag.de/javainsel9/javainsel_21_005.htm
    http://javabeginners.de/Netzwerk/Erreichbarkeit_eines_Hosts.php
    http://stackoverflow.com/questions/12575068/how-to-get-the-result-of-onpostexecute-to-main-activity-because-asynctask-is-a
 */

/**
 * Klasse, welche das Activity für die Anmeldung realisiert
 */
public class ConnectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    /**
     * Anzahl der Messstationen
     */
    public static String Stationen;
    /**
     * Messstation, an die der User sich anmelden will
     */
    public static int gewählteStation = -1;
    private Context context;
    public static long startzeit=System.currentTimeMillis();
    /**
     * Methode, die bei Erstellung des Activities aufgerufen wird
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //onCreate-Void der Superklasse AppCompatActivity ausführen
        super.onCreate(savedInstanceState);
        //per XML generierte GUI laden
        setContentView(R.layout.activity_connection);
        //Spinner sollen schwarze Schrift haben
        //mit "Verbinden" gekennzeichneten Button finden...
        Button verbinden = (Button) findViewById(R.id.verbinden_button);
        context = this.getApplicationContext();
        //... und ihm die aktuelle Klasse als onClickListener zuweisen --> um bei Klick auf ihn Aktion ausführen zu können
        verbinden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //EditText zur IP-Eingabe adressieren...
                EditText ip = (EditText) findViewById(R.id.ip_Eingabe);
                //... und die eingegebene IP einlesen
                IP_ADRESSE = ip.getText().toString();
                //Wenn die IP-Adresse gültig ist...
                if (ipgültig(IP_ADRESSE)) {
                    //... und das Netzwerk verfügbar...
                    if (isNetworkAvailable()) {
                        //... wird eine neue Instanz der Klasse HTTP_Connection erstellt, welche nun eine Verbindung zum Script Abfrage_Stationen.php aufbaut und damit die Anzahl der verfügbaren
                        //Messstationen ermittelt; Übergabe des BaseContext, damit die Klasse Toasts ausgeben kann
                        HTTP_Connection conn = new HTTP_Connection("http://" + IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Stationen.php", true, IP_ADRESSE, getBaseContext());
                        //nach Abschluss der Operationen von conn das Ergebnis der Abfrage durch das Script in die aktuelle Klasse importieren
                        conn.delegate = new AsyncResponse() {
                            @Override
                            public void processFinish(String output) {
                                //speichert, ob Fehler aufgetreten sind
                                boolean verbunden = true;
                                int stationen = 0;
                                //Zahl der Stationen speichern
                                Stationen = output;
                                //versuchen, das übermittelte Resultat in eine ganzzahl umzuwandeln
                                try {
                                    stationen = Integer.parseInt(Stationen);
                                } catch (NumberFormatException e) {
                                    //Resultat konnte nicht in eine Zahl umgewandelt werden --> es ist keine Zahl --> es ist keine Zahl an dieser Stelle in der Datenbank eingetragen --> Hauptanwendung
                                    //(welche dafür zuständig ist) läuft nicht --> Fehlermeldung ausgeben,...
                                    Toast.makeText(context, "Die Hauptanwendung \"Android_Connector\" wird nicht ausgeführt. Bitte versuchen Sie es erneut, wenn die Sitzung gestartet wurde.", Toast.LENGTH_LONG).show();
                                    //... und speichern, dass Fehler aufgetreten sind
                                    verbunden = false;
                                }
                                //sind keine Fehler aufgetreten...
                                if (verbunden) {
                                    System.out.println("Anzahl der Stationen: " + Stationen);
                                    //TextView für die Anmeldung sichtbar machen
                                    TextView view = (TextView) findViewById(R.id.Messstation_TextView);
                                    view.setVisibility(View.VISIBLE);
                                    Spinner s = (Spinner) findViewById(R.id.messstation_wahl);
                                    //Spinner mit den verfügbaren Messstationen füllen

                                    //arraySpinner initialisieren
                                    arraySpinner = new String[stationen];
                                    //ArrraySpinner erhält Werte von 1 bis zur Anzahl der Messstationen -> damit Namen der Messstationen
                                    for (int i = 0; i < Integer.parseInt(Stationen); i++) {
                                        arraySpinner[i] = "" + (i + 1);
                                    }
                                    //ArrayAdapter bekommt Werte von ArraySpinner zugewiesen...
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                                            R.layout.simple_spinner_item, arraySpinner);
                                    //... und wird dem Spinner zugewiesen --> Spinnr erhält die in arraySpinner gespeicherten Werte --> Spinner zeigt Stationen an
                                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                                    s.setAdapter(adapter);
                                    //erste Station auswählen
                                    s.setSelection(0);
                                    //Spinner soll schwarze Texte anzeigen
                                    s.setSelection(0, true);
                                    /*View v = s.getSelectedView();
                                    ((TextView)v).setTextColor(Color.BLACK);*/
                                    //Spinner anzeigen
                                    s.setVisibility(View.VISIBLE);
                                    //Verbindung hergestellt --> Button "Verbinden" führt nun keine Aktionen mehr aus
                                    Button verbinden = (Button) findViewById(R.id.verbinden_button);
                                    verbinden.setOnClickListener(null);
                                    //zur Sicherheit: KeyListener der IP-Eingabe wird auch genullt
                                    EditText ip = (EditText) findViewById(R.id.ip_Eingabe);
                                    ip.setKeyListener(null);
                                    //Button zur Anmeldung an die Messstation anzeigen
                                    wahl.setVisibility(View.VISIBLE);
                                    //Anzeigen der Buttons zum Eintragen
                                    findViewById(R.id.startButton).setVisibility(View.VISIBLE);
                                    findViewById(R.id.stoppButton).setVisibility(View.VISIBLE);
                                    findViewById(R.id.info_Start_Stopp).setVisibility(View.VISIBLE);
                                }
                            }
                        };
                        //Abfrage ausführen
                        conn.execute("params");
                    }
                    //Wird aufgerufen, wenn das Netzwerk nicht verfügbar ist
                    else {
                        Toast.makeText(context, "Verbindung zum Server kann nicht hergestellt werden: Netzwerk nicht verfügbar!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    //wird bei ungültiger IP aufgerufen
                    Toast.makeText(context, "Die eingegebene URL ist keine gültige IPv4-URL oder kann nicht erreicht werden. Bitte geben Sie die URL korrekt ein und versuchen Sie es erneut!", Toast.LENGTH_LONG).show();
                }
            }

        });
        //
        TextView v = (TextView) findViewById(R.id.Messstation_TextView);
        //TextView, Spinner und Button für die Anmeldung an die Station zu Beginn ausblenden (Zahl der Stationen unbekannt --> Spinner leer)
        v.setVisibility(View.GONE);
        Spinner s = (Spinner) findViewById(R.id.messstation_wahl);
        s.setVisibility(View.GONE);
        wahl = (Button) findViewById(R.id.button);
        wahl.setVisibility(View.INVISIBLE);
        //Anmelden-Button ebenfalls diese Klasse als oOnClickListener zuordnen
        wahl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Spinner adressieren
                Spinner s = (Spinner) findViewById(R.id.messstation_wahl);
                //ermitteln, welche Station gewählt wurde, Index = Nummer
                gewählteStation = s.getSelectedItemPosition();
                //prüfen, ob der Wettkampf schon gestartet wurde --> hier, da es sich sonst mit der Abfrage über die Nummer der Stationen in die Quere kommen würde
                if (isNetworkAvailable()) {
                    abfrage_Gestartet = true;
                    HTTP_Connection conn = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_gestartet.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                    System.out.println("Versuche, gestartet-Abfrage zu ermitteln mit: "+"http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_gestartet.php");
                    //Ergebnis der Abfrage an diese Klasse liefern
                    conn.delegate = new AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            //Flagge zurücksetzen
                            abfrage_Gestartet = false;
                            //entsprechenden Wert speichern
                            if (output.equals("true")) {
                                gestartet = true;
                            }

                            if (output.equals("false")) {
                                gestartet = false;
                            }
                            //Startzeit soll ermittelt werden
                            abfrageStartzeit=true;
                            //Startzeit abfragen
                            //prüfen, ob Netzwerkzugriff
                            if (isNetworkAvailable()) {
                                //Aufruf des Scriptes vorbereiten, welches die Startzeit ermittelt
                                HTTP_Connection conn = new HTTP_Connection("http://" + ConnectionActivity.IP_ADRESSE + "/AndroidConnectorAppHTTPScripts/Abfrage_Startzeit.php", true, ConnectionActivity.IP_ADRESSE, getBaseContext());
                                //Ergebnis der Abfrage an diese Klasse liefern
                                conn.delegate = new AsyncResponse() {
                                    @Override
                                    public void processFinish(String output) {
                                        //initiales Attribut zurücksetzen
                                        abfrageStartzeit=false;
                                        //keine Fehler bei Umwandlung aufgetreten
                                        boolean noerror = true;
                                        //Versuch, Startzeit zu übernehmen
                                        try{
                                            startzeit = Long.parseLong(output);
                                        }catch (NumberFormatException e){
                                            //Startzeit kann nicht in Zahl umgewandelt werden --> nur bei Verbindungsfehler möglich, da diese Methode nur aufgerufen wird, falls eine Startzeit existiert
                                            CONNECTION_ERROR=true;
                                            System.out.println(output+" ist keine long.");
                                            //Fehler aufgetreten
                                            noerror=false;
                                            //Fehlermeldung ausgeben
                                            Toast.makeText(context, "Der Wettkampf ist noch nicht gestartet und Sie können noch keine Zeiten eintragen oder ein Netzwerkfehler ist aufgetreten. Bitte überprüfen Sie Ihre Netzwerkverbindung und versuchen Sie es später erneut!", Toast.LENGTH_LONG).show();
                                        }
                                        System.out.println("Startzeit: "+startzeit);
                                        //nur bei erfolgreicher Verbindung...
                                        if(noerror){
                                            //...nächstes Activity starten, in dem nun Zeiten für die einzelnen Teilnehmer eingetragen werden können
                                            startActivity(new Intent(context, MainActivity.class));
                                        }
                                    }
                                };
                                //Abfrage ausführen
                                conn.execute("params");
                            }
                        }

                    };
                    //Abfrage ausführen
                    conn.execute("params");
                }
            }

        });
        //Button zum Starten adressieren...
        Button startButton = (Button) findViewById(R.id.startButton);
        //... ihn zu Beginn ausblenden ...
        startButton.setVisibility(View.INVISIBLE);
        //... und ihm einen OnClickListener zuweisen, der die geforderte Activity startet
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context,StartActivity.class));
            }
        });
        //Button zum Stoppen adressieren...
        Button stoppButton = (Button) findViewById(R.id.stoppButton);
        //... ihn anfangs ausblenden...
        stoppButton.setVisibility(View.INVISIBLE);
        //... und ihm einen OnClickListener zuweisen, der die geforderte Activity startet
        stoppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, StoppActivity.class));
            }
        });

        //Hinweis zum Stoppen ebenfalls ausblenden
        TextView view = (TextView) findViewById(R.id.info_Start_Stopp);
        view.setVisibility(View.INVISIBLE);
    }

    /**
     * Button, mit dem der User sich an eine Messstation anmeldet
     */
    Button wahl;
    /**
     * vom User eingegebene IP-Adresse des Hosts
     */
    static String IP_ADRESSE;
    /**
     * true, wenn gerade geprüft wird, ob der Wettkampf schon gestartet wurde, sonst false
     */
    private boolean abfrage_Gestartet = false;
    /**
     * true, wenn der Wettkampf gestartet ist, sonst false
     */
    public static  boolean gestartet = false;

    /**
     * Prozedur, die aufgerufen wird, wenn auf einen der Buttons geklickt wird
     *
     * @param v der Button, der geklickt wurde
     */
    /*
    @Override
    public void onClick(View v) {
        //Fall, dass der Button "Verbinden" geklickt wurde...
        if (v == findViewById(R.id.verbinden_button)) {

            //wird aufgerufen, wenn der andere Button geklickt wurde, mit dem man sich an einer Messstation anmeldet
        } else {

        }
    }
*/
    /**
     * Prüft, ob eine eingegebene IP-Adresse formal gültig ist
     *
     * @param ipAdresse die zu prüfende IP-Adresse
     * @return true für gültig und false für ungültig
     */
    private boolean ipgültig(String ipAdresse) {
        //IP-Adresse in einzelne Zahlen aufspalten
        String[] ip = ipAdresse.split("\\.");
        //Prüfen, ob die IP korrekterweise aus 4 oder 6 Zahlen besteht (IPv4/IPv6)), sonst ist sie ungültig
        if (!(ip.length == 4 || ip.length == 6)) {
            System.out.println("Besteht aus zu wenig Zahlen oder Punkten!");
            return false;
        }
        String temp;
        int teil = 0;
        //für jede der Zahlen...
        for (int i = 0; i < ip.length; i++) {
            temp = ip[i];
            //versuchen, diese in eine Integer-Zahl umzuwandeln
            try {
                teil = Integer.parseInt(temp);
            } catch (NumberFormatException e) {
                //NumberFormatException --> Teil der IP ist keine Zahl --> IP ist ungültig (Anmerkung: Punkte wurden bei String[].split entfernt)
                System.out.println("Teilstring" + temp + "ist keine Zahl!");
                return false;
            }
            //prüfen, ob Teilzahl im richtigen Größenintervall liegt --> wenn nicht: IP ungültig
            if (teil < 0 || teil > 255) {
                System.out.println("Teilzahl im ungültigen Bereich!");
                return false;
            }
        }
        //wenn Methode bis hier läuft, sind alle Kriterien erfüllt --> IP formal gültig
        return true;
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
     * enthält die Nummern der Messstationen
     */
    private String[] arraySpinner;

    private boolean abfrageStartzeit = false;
    /**
     * Wird aufgerufen, sobald Instanz der Klasse HTTP_Connection fertig gelaufen ist
     *
     * @param output der Inhalt der von HTTP_Connection eingelesenen Webseite, welche von einem der PHP-Scripts (Abfrage_Stationen.php) generiert wurde
     */
    /*
    @Override
    public void processFinish(String output) {
        System.out.println("ankommt" + output);
        //beim Auftreten von Fehlern wird output dieser Wert zugewiesen
        if (output.equals("CONNECTION_ERROR")) {
            //Fehlermeldung per Toast ausgeben
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden!"
                    + " Bitte prüfen Sie, dass Sie die IP-Adresse des Servers korrekt eingegeben haben und mit dem korrekten Netzwerk verbunden sind!", Toast.LENGTH_LONG).show();
        }
        //Prüfen, ob SQL- oder Verbindungsfehler aufgetreten sind
        else if (!this.SQL_ERROR && !this.CONNECTION_ERROR && !output.equals("CONNECTION_ERROR")) {
            if (!abfrage_Gestartet&&!abfrageStartzeit) {

            }
            //prüfen, ob der Wettkampf bereits läuft
            else if (abfrage_Gestartet&&!abfrageStartzeit) {
            }
            //Startzeit des Wettkampfes ermitteln
            else if(abfrageStartzeit&&!abfrage_Gestartet){

            }
        }
        //SQL-Fehler aufgetreten --> Fehler in der Abfrage --> internes Problem
        if (this.SQL_ERROR) {
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden: Es ist ein internes Problem aufgetreten! Bitte benachrichtigen Sie den Entwickler!", Toast.LENGTH_LONG).show();
        }
        //Verbindungsfehler--> falsches Netzwerk oder falsche IP oder Server nicht erreichbar
        if (this.CONNECTION_ERROR || output.equals("CONNECTION_ERROR")) {
            System.out.println("Verbindungsfehler");
            Toast.makeText(this, "Verbindung zum Server kann nicht hergestellt werden!"
                    + " Bitte prüfen Sie, dass Sie die IP-Adresse des Servers korrekt eingegeben haben und mit dem korrekten Netzwerk verbunden sind!", Toast.LENGTH_LONG).show();
        }

        //Fehlercodes zurücksetzen, um es neu probieren zu können
        this.SQL_ERROR = false;
        this.CONNECTION_ERROR = false;
    }
*/
    /**
     * true, wenn bei der Abfrage eines Scripts Verbindungsfehler aufgetreten sind, sonst false
     */
    public static boolean CONNECTION_ERROR = false;
    /**
     * true, wenn eine SQL-Abfrage nicht funktioniert hat, sonst false
     */
    public static boolean SQL_ERROR = false;
    /**
     * true, wenn der Server unter der eingegebenen IP nicht zu erreichen war, sonst false
     */
    public static boolean URL_Error = false;
    /**
     * speichert, ob bereits eine Selektion im Spinner getroffen wurde --> eine Selektion erfolgt durch Programm
     */
    boolean einmalgewählt = false;
    //nachfolgend: Implementierung eines SelectionListeners für den Spinner --> Anmeldung könnte alternativ ohne Klick, sondern durch die Auswahl der Station im Spinner laufen

    /**
     * Wird aufgerufen, wenn ein Element des Spinners gewählt wurde
     *
     * @param adapterView alle Elemente des Spinners
     * @param view        das ausgewählte Element
     * @param i           dessen Index
     * @param l
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //Station gewählt, die die nummer des Indexes hat --> Index speichern
        //schwarze Items
        //((TextView) ((Spinner) findViewById(R.id.messstation_wahl)).getSelectedView()).setTextColor(Color.BLACK);
        System.out.println("Eingeloggt auf Station: " + i);
        //eine Selektion wird durch das programm durchgeführt --> ab der zweiten Selektion erst anmelden
        if (einmalgewählt) {
            //nächstes Activity starten
            startActivity(new Intent(this, MainActivity.class));
        }
        //beim nächsten mal nächstes Activity starten
        einmalgewählt = true;

    }

    /**
     * Wird ausgeführt, wenn Element 0 im Spinner ausgewählt wurde
     *
     * @param adapterView alle Elemente
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //speichern, dass die erste Messstation gewählt wurde...
        gewählteStation = 0;
        //... und das nächste Activity starten
        startActivity(new Intent(this, MainActivity.class));
    }
}
