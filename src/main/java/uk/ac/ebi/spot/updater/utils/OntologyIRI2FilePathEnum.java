package uk.ac.ebi.spot.updater.utils;

/**
 * Created by catherineleroy on 28/07/2015.
 */
public enum OntologyIRI2FilePathEnum {

    ORDO_AXIOM("http://www.ebi.ac.uk/efo/efoordoaxioms.owl","efoordoaxioms.owl"),
    ORDO_MODULE("http://www.orpha.net/ontology/efo_ordo_module.owl","efo_ordo_module.owl"),
    DISEASE_MODULE("http://www.ebi.ac.uk/efo/efoDiseaseModule","efo_disease_module.owl"),
    DISEASE_AXIOM("http://www.ebi.ac.uk/efo/efoDiseaseAxiom","efoDiseaseAxioms.owl"),
    EFO("http://www.ebi.ac.uk/efo","efo_release_candidate.owl");

    private String iri;
    private String fileName;


    private OntologyIRI2FilePathEnum(String iri, String fileName){
        this.iri = iri;
        this.fileName = fileName;
    }
    public String getIri(){
        return iri;
    }
    public String getFileName(){
        return fileName;
    }

}
