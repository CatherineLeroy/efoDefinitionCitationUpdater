package uk.ac.ebi.spot.updater;

import uk.ac.ebi.spot.updater.bioportal.restClient.BioportalClassRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.ebi.spot.updater.bioportal.exceptions.BioportalDeadException;
//import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.ebi.spot.updater.utils.Efo2Xref;
import uk.ac.ebi.spot.updater.utils.OntologyEnum;
import uk.ac.ebi.spot.updater.utils.OntologyIRI2FilePathEnum;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Created by catherineleroy on 26/08/2015.
 */
public class XrefGetterCallable  implements Callable<String>{


    private OWLClass class2Update;
    private String ontologyFileDirectory;
    private String ontologyFileName;
//    static Logger LOG = Logger.getLogger(EfoUpdater.class.getName());
    public final static String BIOPORTAL_DEAD_MESSAGE = "NOT_FOUND_AS_BIOPORTAL_CRASHED";

    public XrefGetterCallable(OWLClass class2Update, String ontologyFileDirectory, String ontologyFileName) {
        this.class2Update = class2Update;
        this.ontologyFileDirectory = ontologyFileDirectory;
        this.ontologyFileName = ontologyFileName;
    }




    public String call() {



        File efoFile = new File(this.ontologyFileDirectory + this.ontologyFileName);

        //Import all owl files (efo_disease_module.owl, efo_ordo_module.owl ...).
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        for (OntologyIRI2FilePathEnum enumeration : OntologyIRI2FilePathEnum.values()) {
            manager.addIRIMapper(
                    new SimpleIRIMapper(IRI.create(enumeration.getIri()),
                            IRI.create("file:" + this.ontologyFileDirectory + enumeration.getFileName())));
        }


        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLOntologyDocumentSource owlOntologyDocumentSource = new FileDocumentSource(efoFile);
        OWLOntology efo = null;
        try {
            efo = manager.loadOntologyFromOntologyDocument(owlOntologyDocumentSource, config);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }


        Efo2Xref efo2Xref = new Efo2Xref(class2Update.getIRI().toString());
        OntologyEnum lastOntologyEnum = null;

        try {

            // Get the ontology containing this class (main efo or efoDiseaseModule).
            OWLOntology containerOntology = getContainerOntology(efo, this.class2Update, manager);

            //Get the label of the class to update/
            String label = getLabel(this.class2Update, containerOntology, factory);
            efo2Xref.setEfoClassLabel(label);
            //Replace the '%' character by the url valid code '%25' to allow safe use when retrieving from bioportal.


            BioportalClassRetriever bioportalClassRetriever = new BioportalClassRetriever();



            //For each of the ontology we want to get the update from (DOID, NCIT...)
            for (OntologyEnum ontologyEnum : OntologyEnum.values()) {

                if(ontologyEnum.getNeedUpdating()) {
                    lastOntologyEnum = ontologyEnum;

                    Collection<String> xrefs = new ArrayList<>();
                    JsonNode bioportalNode;

                    //Search in bioportal with the class label
                    bioportalNode = bioportalClassRetriever.getJsonNode(label, ontologyEnum, true);

                    // If one or more bioportal entries were found for the given label and ontologyEnum, then fetch those bioportal
                    // entries ids and add them to our list of xrefs.
                    // (Note that the getXrefs method will only return xref from the bioportal entries where our label was
                    // an exact match to the bioportal entry preflabel).
                    if (bioportalNode != null) {
                        xrefs = getXrefs(bioportalNode, ontologyEnum);
                    }

                    //If no bioportal entry was found for that label or the bioportal entry found was only matching our label
                    // through the bioportal synonym then querry bioportal using all the alternative_term.
                    if (xrefs.size() == 0) {
                        URL url = new URL("http://www.ebi.ac.uk/efo/alternative_term");
                        OWLAnnotationProperty alternativeTermAnnotProperty = factory.getOWLAnnotationProperty(IRI.create(url));
                        for (OWLAnnotation annotation : this.class2Update.getAnnotations(containerOntology, alternativeTermAnnotProperty)) {
                            if (annotation.getValue() instanceof OWLLiteral) {
                                OWLLiteral val = (OWLLiteral) annotation.getValue();
                                String synonym = val.getLiteral();
                                bioportalNode = bioportalClassRetriever.getJsonNode(synonym, ontologyEnum, true);

                                // If one or more bioportal entries were found for the given alternative_term and ontologyEnum,
                                // then fetch those bioportal entries ids and add them to our list of xrefs.
                                // (Note that the getXrefs method will only return xref from the bioportal entries where
                                // our alternative_term was an exact match to the bioportal entry preflabel).
                                if (bioportalNode != null) {
                                    xrefs.addAll(getXrefs(bioportalNode, ontologyEnum));
                                }
                            }
                        }
                    }

                    //Add the xrefs to the efo2Xref object to link them to the correct ontologyEnum they belong to.
                    if (xrefs.size() > 0) {
                        efo2Xref.addXrefForOntology(xrefs, ontologyEnum);
                    }
                }
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BioportalDeadException e) {
            Collection<String> xrefs = new ArrayList<>();
            xrefs.add(BIOPORTAL_DEAD_MESSAGE);
            efo2Xref.addXrefForOntology(xrefs, lastOntologyEnum);
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return efo2Xref.toString();

    }

    private Collection<String> getXrefs(JsonNode bioportalNode, OntologyEnum ontologyEnum){
        Collection<String> xrefExternalIds = new ArrayList<>();
        //Going through the collection of term of the bioportalNode
        for (JsonNode collection : bioportalNode.get("collection")) {
            //If the match by bioportal wasn't done on prefLabel but on synonyms discard.
            // This is to avoid the following problem :
            //      In efo the parent term : Microcephalic primordial dwarfism
            //      as child : Seckel syndrome
            // When searching for "Microcephalic primordial dwarfism" in DOID with bioportal rest api you get "Seckel
            // syndrome" as "Microcephalic primordial dwarfism" is a synonym of "Seckel Syndrome" and bioportal tries to
            // match with prefLabel and synonyms.
            // ex : http://data.bioontology.org/search?q=Microcephalic%20primordial%20dwarfism&ontologies=DOID&require_exact_match=true
//            LOG.debug("\tmatchType = " + collection.get("matchType").asText() );
//            LOG.debug("\tcollection.get(\"@id\").asText()" + collection.get("@id").asText());
            if ("prefLabel".equals(collection.get("matchType").asText())) {
                String externalId = formatExternalIdForEfo(collection.get("@id").asText(), ontologyEnum);
                xrefExternalIds.add(externalId);
//                LOG.debug("\texternalId = " + externalId);

            }
        }
        return xrefExternalIds;
    }

    /**
     * Given a full IRI class id ( ex : http://purl.obolibrary.org/obo/DOID_5223) it returns the short version of the idea ( ex : DOID:5223)
     * which we use as value of the definition_citation annotations for efoClasses.
     * @param fullIRIClassId a full IRI class id (ex : http://purl.obolibrary.org/obo/DOID_5223)
     * @param ontology the ontology to which the fullIRIClassId refers too
     * @return the short version of the class id (ex : DOID:5223)
     */
    public String formatExternalIdForEfo(String fullIRIClassId, OntologyEnum ontology){

        //http://purl.obolibrary.org/obo/DOID_5223     DOID:5223   (DOID)
        if(OntologyEnum.HUMAN_DESEASE_ONTOLOGY.equals(ontology)){
            fullIRIClassId = fullIRIClassId.replace("http://purl.obolibrary.org/obo/", "");
            fullIRIClassId = fullIRIClassId.replace("_", ":");
            //http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C3014     (NCIT) NCIt:C3014
        }else if(OntologyEnum.NCI_THESAURUS.equals(ontology)) {
            fullIRIClassId = fullIRIClassId.replace("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "NCIt:");
            //http://purl.bioontology.org/ontology/ICD9CM/606.0     ICD9:606.0
        }else if(OntologyEnum.INTERNATIONAL_CLASSIFICATION_OF_DISEASES.equals(ontology)) {
            fullIRIClassId = fullIRIClassId.replace("http://purl.bioontology.org/ontology/ICD9CM/", "ICD9:");



            //http://purl.bioontology.org/ontology/SNOMEDCT/129103003 SNOMEDCT:129103003  SNOMEDCT
        }else if(OntologyEnum.SYSTEMATIZED_NOMEMCLATURE_OF_MEDICINE.equals(ontology)) {
            fullIRIClassId = fullIRIClassId.replace("http://purl.bioontology.org/ontology/SNOMEDCT/", "SNOMEDCT:");


            //http://purl.bioontology.org/ontology/MESH/D007246   MSH:D007246
        }else if(OntologyEnum.MEDICAL_SUBJECT_HEADINGS.equals(ontology)) {
            fullIRIClassId = fullIRIClassId.replace("http://purl.bioontology.org/ontology/MESH/", "MSH:");

        }
        return fullIRIClassId;
    }

    /**
     * Returns the label of the given owlClass.
     *
     * @param owlClass - the owlClass for which you want the label
     * @param ontology - the ontology the owlClass belongs
     * @param factory - the ontology factory.
     *
     * @return the label if found, null otherwise.
     */
    public String getLabel(OWLClass owlClass, OWLOntology ontology, OWLDataFactory factory) {
        String label = null;
        OWLAnnotationProperty labelAnnotProperty = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
        for (OWLAnnotation annotation : owlClass.getAnnotations(ontology, labelAnnotProperty)) {
            if (annotation.getValue() instanceof OWLLiteral) {
                OWLLiteral val = (OWLLiteral) annotation.getValue();
                label = val.getLiteral();
            }
        }
        return label;
    }

    /**
     * Given an ontology and an owl class it will return the ontology that contains the given class2Update. This ontology
     * might be the mainOntology or an ontology imported by the mainOntology. I filter out the ontology which IRI contains
     * the word "axiom" as in this case we're only interested in the "module" ontology (this method is a bit efo specific).
     *
     * @param mainOntology - the ontology that either contains the class2Update or whose imported ontology contains it.
     * @param class2Update - the class for which you want to find the container ontology
     * @param manager - the containerOntology manager.
     *
     * @return null if nothing was found or the found container ontology.
     */
    public OWLOntology getContainerOntology(OWLOntology mainOntology, OWLClass class2Update, OWLOntologyManager manager) {
        if (mainOntology.containsClassInSignature(class2Update.getIRI())) {
            return mainOntology;
        } else {
            for (OWLOntology importedOntology : manager.getImports(mainOntology)) {
                if (importedOntology.containsClassInSignature(class2Update.getIRI()) && importedOntology.getOntologyID().getOntologyIRI().toString().equals(OntologyIRI2FilePathEnum.DISEASE_MODULE.getIri())) {
                    return importedOntology;
                }
            }
        }
        return null;
    }






}
