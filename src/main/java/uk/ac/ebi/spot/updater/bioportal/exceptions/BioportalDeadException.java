package uk.ac.ebi.spot.updater.bioportal.exceptions;

/**
 * Created by catherineleroy on 26/01/2015.
 */
public class BioportalDeadException extends Exception {
    public static String BIOPORTAL_SORRY_MESSAGE = "We're sorry but something has gone wrong. We have been notified of this error.";

    public BioportalDeadException() {}

    //Constructor that accepts a message
    public BioportalDeadException(String message)
    {
        super("Sorry it seems that bioportal is dead : " + message);
    }
}
