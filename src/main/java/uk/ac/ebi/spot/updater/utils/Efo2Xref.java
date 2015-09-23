package uk.ac.ebi.spot.updater.utils;

import java.util.*;

/**
 * Created by catherineleroy on 29/07/2015.
 */
public class Efo2Xref {
    private String efoClassLabel;
    private String efoClassIRI;
    private Map<String,String> ontology2xrefIds = new HashMap<String, String>();

    public Efo2Xref(String efoClassIRI){
        this.efoClassIRI = efoClassIRI;
    }

    public void addXrefForOntology(Collection<String> xrefs, OntologyEnum ontologyEnum){
        String xrefsConcatenation = "";
        int i = 0;
        for(String xref : xrefs){
            if(i == 0){
                xrefsConcatenation = xrefsConcatenation + xref;
                i++;
            }else{
                xrefsConcatenation = xrefsConcatenation + "," + xref;
            }
        }
        ontology2xrefIds.put(ontologyEnum.getBioPortalAcronym(), xrefsConcatenation);
    }

    public void setEfoClassLabel(String efoClassLabel) {
        this.efoClassLabel = efoClassLabel;
    }

    public String getEfoClassLabel() {
        return efoClassLabel;
    }

    public String getEfoClassIRI() {
        return efoClassIRI;
    }

    public Map<String, String> getOntology2xrefIds() {
        return ontology2xrefIds;
    }


    public void setOntology2xrefIds(Map<String, String> ontology2xrefIds) {
        this.ontology2xrefIds = ontology2xrefIds;
    }


    public static String getHeader(){
        String header = "IRI\tlabel";
        List<String> externalOntologies = new ArrayList<String>();
        for(OntologyEnum ontologyEnum : OntologyEnum.values()){
            if(ontologyEnum.getNeedUpdating()) {
                externalOntologies.add(ontologyEnum.getBioPortalAcronym());
            }
        }
        Collections.sort(externalOntologies);
        for(String externalOntologie : externalOntologies){
            header = header + "\t" + externalOntologie;
        }
        return header;
    }

    public String toString(){

        List<String> externalOntologies = new ArrayList<String>();
        for(OntologyEnum ontologyEnum : OntologyEnum.values()){
            if(ontologyEnum.getNeedUpdating()) {
                externalOntologies.add(ontologyEnum.getBioPortalAcronym());
            }
        }
        Collections.sort(externalOntologies);

        String output = this.efoClassIRI + "\t" + this.efoClassLabel;
        for(String key : externalOntologies){
            if(this.ontology2xrefIds.containsKey(key)) {
                output = output + "\t" + this.ontology2xrefIds.get(key);
            }else{
                output = output + "\tNONE";
            }
        }
        return output;
    }
}
