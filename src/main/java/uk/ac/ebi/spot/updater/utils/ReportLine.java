package uk.ac.ebi.spot.updater.utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by catherineleroy on 21/01/2015.
 */
public class ReportLine {
    private String efoClassLabel;
    private String efoClassIRI;
    private Collection<String> removedIds = new ArrayList<String>();
    private Collection<String> addedIds = new ArrayList<String>();

    public String getEfoClassLabel() {
        return efoClassLabel;
    }

    public void setEfoClassLabel(String efoClassLabel) {
        this.efoClassLabel = efoClassLabel;
    }

    public String getEfoClassIRI() {
        return efoClassIRI;
    }

    public void setEfoClassIRI(String efoClassIRI) {
        this.efoClassIRI = efoClassIRI;
    }

    public void addRemovedId(String id){
        this.removedIds.add(id);
    }
    public Collection<String> getRemovedIds(){
        return this.removedIds;
    }


    public void addAddedId(String id){
        this.addedIds.add(id);
    }
    public Collection<String> getAddedIds(){
        return this.addedIds;
    }


    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append(getEfoClassIRI() + "\t");
        buffer.append(getEfoClassLabel()+ "\t");

        int count = 0;
        for(String removedId : this.getRemovedIds()){
            if(count==0) {
                buffer.append(removedId);
            }else{
                buffer.append("," + removedId);

            }
            count++;
        }
        buffer.append("\t");

        count=0;
        for(String addedId : this.getAddedIds()){
            if(count==0) {
                buffer.append(addedId);
            }else{
                buffer.append("," + addedId);

            }
            count++;

        }

        return buffer.toString();
    }

    public static String getHeader(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("EFO class IRI\t");
        buffer.append("EFO class label\t");
        buffer.append("Removed Def Citation\t");
        buffer.append("Added Def Citation\t");
        return buffer.toString();
    }
}
