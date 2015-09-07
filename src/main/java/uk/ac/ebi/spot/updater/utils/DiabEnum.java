package uk.ac.ebi.spot.updater.utils;

/**
 * Created by catherineleroy on 20/01/2015.
 */
public enum DiabEnum {

    //HUMAN_DESEASE_ONTOLOGY("DOID", "http://www.ebi.ac.uk/efo/DOID_definition_citation"),
    //NCI_THESAURUS("NCIT", "http://www.ebi.ac.uk/efo/NCI_Theasurus_definition_citation"),
    //International Classification of Diseases, Version 9 - Clinical Modification
//    INTERNATIONAL_CLASSIFICATION_OF_DISEASES("ICD9CM", "http://www.ebi.ac.uk/efo/ICD9_definition_citation"),
    //Systematized Nomenclature of Medicine - Clinical Terms
    //SYSTEMATIZED_NOMEMCLATURE_OF_MEDICINE("SNOMEDCT", "http://www.ebi.ac.uk/efo/SNOMEDCT_definition_citation"),
    //Medical Subject Headinds
//   MEDICAL_SUBJECT_HEADINGS("MESH", "http://www.ebi.ac.uk/efo/MSH_definition_citation");

    INTERNATIONAL_CLASSIFICATION_OF_DISEASES("ICD9CM", "http://purl.obolibrary.org/obo/ICD9_definition_citation"),
    MEDICAL_SUBJECT_HEADINGS("MESH", "http://purl.obolibrary.org/obo/MSH_definition_citation");


    private String bioPortalAcronym;
    private String efoDefinitionCitationIRI;

    private DiabEnum(String bioPortalAcronym, String efoDefinitionCitation){
        this.bioPortalAcronym = bioPortalAcronym;
        this.efoDefinitionCitationIRI = efoDefinitionCitation;
    }

    public String getBioPortalAcronym() {
        return bioPortalAcronym;
    }

    public String getEfoDefinitionCitationIRI() {
        return efoDefinitionCitationIRI;
    }

}
