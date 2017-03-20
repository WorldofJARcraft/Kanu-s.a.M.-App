package de.ackermann.eric.androidconnectorapphttp;

/**
 * Created by Eric on 23.07.2016. Interface für die Übertragung des Ergebnisses einer AsyncTask-Berechnung an eine andere Klasse.
 */
public interface AsyncResponse {
    void processFinish(String output, long durationMillis, String url);
}
