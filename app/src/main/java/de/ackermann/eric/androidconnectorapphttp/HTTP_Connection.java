package de.ackermann.eric.androidconnectorapphttp;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 * Created by Eric on 23.07.2016 nach http://stefan-draeger-software.de/blog/android-app-mit-mysql-datenbank-verbinden/.
 * Android lässt keine Netzwerkaktionen auf dem Mainthread zu, deshalb wird AsyncTask hier beerbt. Dieser erzeugt einen zweiten Thread, der alle Scriptaufrufe ausführen kann.
 * Die Parameter in den Winkelklammern stehen für die Rückgabetypen der folgenden Methoden.
 */
public class HTTP_Connection extends AsyncTask<String, Void, String> {
    /**
     * Möglichkeit, das Abfrageergebnis aus dieser Klasse in eine andere Klasse zu übertragen. Darf nicht null sein.
     */
    public AsyncResponse delegate = null;
    /**
     * entscheidet, ob der eingelesene Output des Scripts weitergegeben wird
     */
    private boolean resultatweitergeben;
    /**
     * speichert die aufzurufende URL
     */
    private String url = "";
    /**
     * speichert die reine IP-Adresse des Servers
     */
    private String orurl;
    //maximale Wiederholungen. Default 10.
    int mMaxRetries = 3;
    /**
     * Hauptkonstruktor der Klasse. Maximalzahl an Verbindungsversuchen ist 10.
     *
     * @param nachricht die komplette aufzurufende Web-Adresse
     * @param delegate  true für Resultat weitergeben, false für Resultat nicht weitergeben
     * @param url       die eigentliche IP-Adresse des Zielservers
     * @param mContext  ein Kontext für die Ausgabe von Toasts
     */
    public HTTP_Connection(String nachricht, boolean delegate, String url, Context mContext) {
        //übergebene Werte in globalen Variablen speichern
        this.url = nachricht;
        resultatweitergeben = delegate;
        orurl = url;
    }
    /**
     * Zusätzlicher Konstruktor, in dem man eine Maximalzahl an versuchen übergeben kann.
     *
     * @param nachricht die komplette aufzurufende Web-Adresse
     * @param delegate  true für Resultat weitergeben, false für Resultat nicht weitergeben
     * @param url       die eigentliche IP-Adresse des Zielservers
     * @param mContext  ein Kontext für die Ausgabe von Toasts
     */
    public HTTP_Connection(String nachricht, boolean delegate, String url, Context mContext, int mMaxRetries) {
        //übergebene Werte in globalen Variablen speichern
        this.url = nachricht;
        resultatweitergeben = delegate;
        orurl = url;
        this.mMaxRetries = mMaxRetries;
    }
    /**
     * Methode, welche die Verbindung zur Webseite herstellt und den Content ausliest. Wird wiederholt, bis die Maximalzahl an versuchen überschritten wird oder eine Antwort eintrifft.
     * @param params unnötige Parameter
     * @return Inhalt der Webseite, wenn eine Antwort eingetroffen ist, sonst null
     * @author Quelle: http://stackoverflow.com/questions/18359039/repeat-asynctask
     */
    protected String repeatInBackground(String... params){
        //Annahme, alles wäre gut gegangen
        boolean keineFehler = true;
        //Initialisierung eines StringBuilders
        StringBuilder sb = new StringBuilder();
        //versuchen, Script zu starten und Daten davon auszulesen
        try {
            if (keineFehler) {
                System.out.println("Versuche, Anfrage an "+url+" zu senden!");
                //String data = URLEncoder.encode("authkey", "UTF-8") + "=" + URLEncoder.encode(AUTHKEY, "UTF-8");
                //übergebene Abrufadresse in eine URL umwandeln
                URL url = new URL(this.url);
                System.out.println(url.toString());
                //Verbindung herstellen
                URLConnection conn = url.openConnection();
                //HTML-Seite soll ausgegeben und  an den Client geschickt werden
                conn.setDoOutput(true);
                //BufferedReader zum Einlesen der Website initialisieren
                BufferedReader reader;
                try {
                    //conn streamt nun die aufgerufene Website, welche ein BufferedReader aufnimmt
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } catch (RuntimeException e) {
                    //allgemeiner Fehler beim Abruf aufgetreten --> Fehler zurückgeben und abbrechen
                    ConnectionActivity.CONNECTION_ERROR = true;
                    ConnectionActivity.URL_Error = true;
                    keineFehler = false;
                    return null;
                } catch (ConnectException e) {
                    //Verbindung nicht möglich --> Fehler zurückgeben und abbrechen
                    ConnectionActivity.CONNECTION_ERROR = true;
                    return null;
                }

                //einzelne Zeile
                String line = null;
                //Zeilen der Website einlesen und einem StringBuilder hinzufügen, solange diese einen Inhalt haben
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    sb.append(line);
                }
            }
            //Augerufen, wenn bereits Fehler aufgetreten sind --> dann Abbruch
            else {
                return null;
            }
        } catch (MalformedURLException e) {
            //Aufruf, wenn URL nicht gültig ist (eigentlich schon geprüft) --> Abbruch, Fehler speichern
            ConnectionActivity.SQL_ERROR = true;
            MainActivity.SQL_ERROR = true;
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            //allgemeiner Fehler -->m Abbruch
            ConnectionActivity.CONNECTION_ERROR = true;
            MainActivity.CONNECTION_ERROR = true;
            e.printStackTrace();
            return null;
        }
        //String zurückgeben, der Inhalt der Website enthält
        return sb.toString();
    }
    /**
     * erster, durch "HTTP_Connection.execute" aufgerufener Void. Startet die Wiederholten Anfragen, bricht sie nach einer Anzahl an Versuchen ab.
     *
     * @param params Parameter für die Ausführung
     * @return Inhalt der eingelesenen Website als String, oder die Notiz "Es ist ein Netzwerkfehler aufgetreten!" bei 10 sinnlosen Versuchen.
     */
    @Override
    protected String doInBackground(String... params) {
        int tries = 0;
        String result = null;

        /* This is the main loop, repeatInBackground will be repeated until result will not be null */
        while(tries++ < mMaxRetries && result == null) {
            try {
                result = repeatInBackground(params);
            } catch (Exception exception) {
                /* You might want to log the exception everytime, do it here. */
                //mException = exception;
                exception.printStackTrace();
            }
        }
        if(result==null)
            return "Es ist ein Netzwerkfehler aufgetreten!";
        return result;
    }

    /**
     * Wird nach der Abfrage aufgerufen.
     * @param result Resultat der Abfrage = Inhalt der Website
     */
    @Override
    protected void onPostExecute(String result) {
        //prüfen, ob das Resultat weiterzugeben ist
        if (resultatweitergeben) {
            //Prozedur processFinish des Interface "AsyncResponse" aufrufen --> dieses ist abstrakt und muss von einer Klasse implementiert werden;
            //delegate muss zudem jene Klasse zugewiesen sein --> dann wird void in dieser Klasse ausgeführt, dem der Inhalt der Website übergeben wird,
            //und so ist der Inhalt der Website in dieser Klasse verfügbar
            delegate.processFinish(result);
            System.out.println("Ergebnis:" + result);
        }
        System.out.println("Ergebnis: "+result);
    }

}
