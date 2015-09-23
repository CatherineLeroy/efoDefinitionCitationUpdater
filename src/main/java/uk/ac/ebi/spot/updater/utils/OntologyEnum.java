package uk.ac.ebi.spot.updater.utils;

/**
 * Created by catherineleroy on 20/01/2015.
 */
public enum OntologyEnum {

    HUMAN_DESEASE_ONTOLOGY("DOID", "http://www.ebi.ac.uk/efo/DOID_definition_citation", true),
    NCI_THESAURUS("NCIT", "http://www.ebi.ac.uk/efo/NCI_Thesaurus_definition_citation", true),
    //International Classification of Diseases, Version 9 - Clinical Modification
    INTERNATIONAL_CLASSIFICATION_OF_DISEASES("ICD9CM", "http://www.ebi.ac.uk/efo/ICD9_definition_citation", true),
    //Systematized Nomenclature of Medicine - Clinical Terms
    SYSTEMATIZED_NOMEMCLATURE_OF_MEDICINE("SNOMEDCT", "http://www.ebi.ac.uk/efo/SNOMEDCT_definition_citation", true),
   //Medical Subject Headinds
    MEDICAL_SUBJECT_HEADINGS("MESH", "http://www.ebi.ac.uk/efo/MSH_definition_citation", true);


    private String bioPortalAcronym;
    private String efoDefinitionCitationIRI;
    private boolean needUpdating;

    private OntologyEnum(String bioPortalAcronym, String efoDefinitionCitation, boolean needUpdating){
        this.bioPortalAcronym = bioPortalAcronym;
        this.efoDefinitionCitationIRI = efoDefinitionCitation;
        this.needUpdating = needUpdating;
    }

    public String getBioPortalAcronym() {
        return bioPortalAcronym;
    }

    public String getEfoDefinitionCitationIRI() {
        return efoDefinitionCitationIRI;
    }
    public boolean getNeedUpdating() {
        return needUpdating;
    }

    public static OntologyEnum getForBioPortalAcronym(String bioPortalAcronym){
        OntologyEnum ontologyEnum = null;
        for(OntologyEnum ontoEnum : OntologyEnum.values()){
            if(bioPortalAcronym.equals(ontoEnum.bioPortalAcronym)){
                return ontoEnum;
            }
        }
        return ontologyEnum;
    }

}
