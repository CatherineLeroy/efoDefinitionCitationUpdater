package uk.ac.ebi.spot.updater.bioportal.exceptions;

/**
 * Created by catherineleroy on 28/01/2015.
 */
public class MalformedDefinitionCitationValue extends Exception{

    public MalformedDefinitionCitationValue() {}

    //Constructor that accepts a message
    public MalformedDefinitionCitationValue(String message)
    {
        super("Sorry it seems that value of the definition citation was malformed : " + message);
    }
}
