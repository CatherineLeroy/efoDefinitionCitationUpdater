package uk.ac.ebi.spot.updater;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;
import uk.ac.ebi.spot.updater.utils.Efo2Xref;
import uk.ac.ebi.spot.updater.utils.OntologyEnum;
import uk.ac.ebi.spot.updater.utils.OntologyIRI2FilePathEnum;
import uk.ac.ebi.spot.updater.utils.ReportLine;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

// programm argument : -dummy true -remove true -ontoDir /Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate -i /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/xrefGetterCaller_ouput.txt -output /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/EfoXrefUpdater_output.txt


//mvn exec:java -Dexec.mainClass=uk.ac.ebi.spot.updater.EfoXrefUpdater -Dexec.args="-dummy true -remove true -ontoDir /Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate -i /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/xrefGetterCaller_ouput.txt -output /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/EfoXrefUpdater_output.txt"
//mvn exec:java -Dexec.mainClass=uk.ac.ebi.spot.updater.EfoXrefUpdater -Dexec.args="-dummy true -remove true -ontoDir /Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate -i /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/xrefs_all.txt -output /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/EfoXrefUpdater_output.txt"


//mvn exec:java -Dexec.mainClass=uk.ac.ebi.spot.updater.EfoXrefUpdater -Dexec.args="-dummy false -remove false -ontoDir /Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate -i /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/xrefGetterCaller_ouput.txt -output /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/EfoXrefUpdater_output.txt"


/**
 * Created by catherineleroy on 25/08/2015.
 */
public class EfoXrefUpdater {

    /*
            The dummy option allows to run the updater without adding or removing any definition_citation. It allows to get
            a report of what would be removed or added if we ran the updater 'for real'.
            */
    static Boolean dummy = null;
    /*
    The remove option allows to add any new definition citation found from bioportal without removing any definition
    citation.
     */
    static Boolean remove = null;

    /**
     * The directory containing the efo_release_candidate.owl and all the owl file it imports.
     */
    static String ontologyDirectory = null;

    static String outputFile = null;
    /**
     * The path to the file containing the terms and the definition citation to be added.
     * This file should have the following format :
     * EFO_TERM_IRI[tab]EFO_TERM_LABEL[tab]DOID_DEFINITION_CITATIONS[tab]ICD9_DEFINITION_CITATIONS[tabl]MSH_DEFINITION_CITATIONS[tab]NCIt_DEFINITION_CITATIONS[tab]SNOMEDCT_DEFINITION_CITATIONS
     * If there is more then one DOID definition citation for example, those must be separated by a coma.
     * <p/>
     * ex :
     * http://www.ebi.ac.uk/efo/EFO_0005262	mitral annular calcification	NONE	NONE	NONE	NONE	NONE
     * http://www.ebi.ac.uk/efo/EFO_0005940	psychotic symptoms	NONE	NONE	NONE	NONE	NONE
     * http://purl.obolibrary.org/obo/HP_0002619	Varicose veins	DOID:799	NONE	MSH:D014648	NONE	NONE
     * http://www.ebi.ac.uk/efo/EFO_0003762	vitamin D deficiency	NONE	ICD9:268	MSH:D014808	NCIt:C114830	SNOMEDCT:34713006
     * http://www.ebi.ac.uk/efo/EFO_0003784	skin pigmentation	NONE	NONE	MSH:D012880	NCIt:C35026	SNOMEDCT:38962007,SNOMEDCT:370172004
     */
    static String inputFile = null;


    public static void main(String[] args) throws OWLOntologyCreationException, IOException, URISyntaxException, OWLOntologyStorageException {
        EfoXrefUpdater efoXrefUpdater = new EfoXrefUpdater();

        efoXrefUpdater.getOptions(args);

        efoXrefUpdater.update();
    }


    public void getOptions(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-dummy".equals(args[i])) {
                if (args[i + 1].equals("true")) {
                    dummy = true;
                } else {
                    if (args[i + 1].equals("false")) {
                        dummy = false;
                    } else {
                        throw new IllegalArgumentException("-dummy option must be equal to true or false.");
                    }
                }
            }

            if ("-remove".equals(args[i])) {
                if (args[i + 1].equals("true")) {
                    remove = true;
                } else {
                    if (args[i + 1].equals("false")) {
                        remove = false;
                    } else {
                        throw new IllegalArgumentException("-dummy option must be equal to true or false.");
                    }
                }
                i++;
            }

            if ("-ontoDir".equals(args[i])) {
                ontologyDirectory = args[i + 1];
                i++;

            }

            if ("-i".equals(args[i])) {
                inputFile = args[i + 1];
                i++;
            }

            if ("-output".equals(args[i])) {
                outputFile = args[i + 1];
                i++;

            }
        }

        if (remove == null) {
            throw new IllegalArgumentException("-remove option must be provided. It's value must be 'true' or 'false'");

        }
        if (dummy == null) {
            throw new IllegalArgumentException("-dummy option must be provided. It's value must be 'true' or 'false'");

        }

        if (ontologyDirectory == null) {
            ontologyDirectory = "/Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/";
        }

        if (!ontologyDirectory.endsWith("/")) {
            ontologyDirectory = ontologyDirectory + "/";
        }


    }


    public void update() throws OWLOntologyCreationException, IOException, URISyntaxException, OWLOntologyStorageException {

        File efoFile = new File(ontologyDirectory + "/efo_release_candidate.owl");
        if (!efoFile.exists()) {
            throw new IllegalArgumentException("Couldn't find file " + ontologyDirectory + "/efo_release_candidate.owl");
        }

        //Import all owl files (efo_disease_module.owl, efo_ordo_module.owl ...).
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        for (OntologyIRI2FilePathEnum enumeration : OntologyIRI2FilePathEnum.values()) {
            manager.addIRIMapper(
                    new SimpleIRIMapper(IRI.create(enumeration.getIri()),
                            IRI.create("file:" + ontologyDirectory + enumeration.getFileName())));
        }

        // Initiate everything for efo.owl searching
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLOntologyDocumentSource owlOntologyDocumentSource = new FileDocumentSource(efoFile);
        OWLOntology efo = manager.loadOntologyFromOntologyDocument(owlOntologyDocumentSource, config);


        Set<OWLOntology> ontologies = manager.getOntologies();
        ontologies.addAll(manager.getImports(efo));
        ontologies.add(efo);


        String header = Efo2Xref.getHeader();
        String[] headerArray = header.split("\t");

        File xrefFile = new File(inputFile);
        if (!efoFile.exists()) {
            throw new IllegalArgumentException("Couldn't find file " + inputFile);
        }

        Collection<ReportLine> reportLines = new ArrayList<>();


        if (xrefFile.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(xrefFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.equals(header)) {
                    String[] fields = line.split("\t");
                    Efo2Xref efo2Xref = new Efo2Xref(fields[0]);
                    efo2Xref.setEfoClassLabel(fields[1]);
                    Map<String, String> ontology2xrefs = new HashMap();
                    for (int i = 2; i < (OntologyEnum.values().length + 2); i++) {
                        String bioportalAcronym = headerArray[i];
                        ontology2xrefs.put(bioportalAcronym, fields[i].replace("\"", ""));
                    }
                    efo2Xref.setOntology2xrefIds(ontology2xrefs);

                    ReportLine reportLine = updateXref(efo2Xref, factory, manager, efo, new ReportLine(), remove, dummy);
                    reportLines.add(reportLine);
                }
            }
        }

        //Output result to output file.
        PrintWriter writer = new PrintWriter(outputFile);
        writer.println(ReportLine.getHeader());
        for (ReportLine reportLine : reportLines) {
            writer.println(reportLine.toString());

        }
        writer.close();
    }


    public ReportLine updateXref(Efo2Xref efo2Xref, OWLDataFactory factory, OWLOntologyManager manager, OWLOntology efoOntology, ReportLine reportLine, Boolean remove, Boolean dummy) throws MalformedURLException, URISyntaxException, OWLOntologyStorageException {
        String efoIri = efo2Xref.getEfoClassIRI();
        System.out.println("\nChecking definition_citation for " + efoIri);
        reportLine.setEfoClassIRI(efoIri);

        OWLClass class2update = factory.getOWLClass(IRI.create(new URL(efoIri)));

        OWLOntology containerOntology = getContainerOntology(efoOntology, class2update, manager);


        String label = efo2Xref.getEfoClassLabel();
        reportLine.setEfoClassLabel(label);

        Map<String, String> ontology2xrefIds = efo2Xref.getOntology2xrefIds();

        Collection<String> preExistingDefCitations = new ArrayList<>();
        Collection<String> addedDefCitations = new ArrayList<>();


        List<OWLOntologyChange> changes = new ArrayList<>();


        for (String ontologyBioportalAcronym : ontology2xrefIds.keySet()) {
            String xrefs = ontology2xrefIds.get(ontologyBioportalAcronym);
            OntologyEnum ontologyEnum = OntologyEnum.getForBioPortalAcronym(ontologyBioportalAcronym);


            Collection<OWLAnnotation> preExistingDefCitAnnotations = getDefinitionCitation(class2update, ontologyEnum, containerOntology, factory);

            for (OWLAnnotation preExistingDefCitAnnotation : preExistingDefCitAnnotations) {
                OWLAxiom owlAxiom = factory.getOWLAnnotationAssertionAxiom(class2update.getIRI(), preExistingDefCitAnnotation);

                OWLLiteral owlLiteral = (OWLLiteral) preExistingDefCitAnnotation.getValue();
                //Feel the Collection of pre-existing definition_citation value.
                preExistingDefCitations.add(owlLiteral.getLiteral().toString());

                //If remove is set to true, remove all existing definition_citation as they will be added later if
                // they still exist in bioportal.

                if (remove) {
                    changes.addAll(manager.removeAxiom(containerOntology, owlAxiom));
                    System.out.println("\tRemove : " + owlLiteral.getLiteral().toString());
                }

            }
            if (!xrefs.contains("NOT_FOUND_AS_BIOPORTAL_CRASHED") && !xrefs.contains("NONE")) {

                //Add new xref(s)
                String[] bioportalXrefs = xrefs.split(",");
                for (String bioportalXref : bioportalXrefs) {
                    URL url = new URL(ontologyEnum.getEfoDefinitionCitationIRI());
                    OWLAnnotationProperty efoDefCitationAnnotProperty = factory.getOWLAnnotationProperty(IRI.create(url));

                    OWLAnnotation newDefCitAnnotation = factory.getOWLAnnotation(efoDefCitationAnnotProperty, factory.getOWLLiteral(bioportalXref));

                    if (remove) {
                        // if remove is set to true it means that we've previously done a clean up of the efo class2update and
                        // removed all it's definition_citation. Therefore we can add all the new ones without doing
                        // any more check.
                        OWLAxiom owlAxiom = factory.getOWLAnnotationAssertionAxiom(class2update.getIRI(), newDefCitAnnotation);//getowlanngetOWLAnnotationAxiom(prop, domain)
                        List<OWLOntologyChange> theChanges = manager.addAxiom(containerOntology, owlAxiom);
                        changes.addAll(theChanges);
                        System.out.println("\tAdding : " + bioportalXref);
                        addedDefCitations.add(bioportalXref);
                    } else {
                        //if remove is set to false, it means that all the old definition_citation have been kept
                        // so we just want to add the one that don't already exist.
                        if (!preExistingDefCitations.contains(bioportalXref)) {
                            OWLAxiom owlAxiom = factory.getOWLAnnotationAssertionAxiom(class2update.getIRI(), newDefCitAnnotation);//getowlanngetOWLAnnotationAxiom(prop, domain)
                            List<OWLOntologyChange> theChanges = manager.addAxiom(containerOntology, owlAxiom);
                            changes.addAll(theChanges);
                            System.out.println("\tAdding : " + bioportalXref + " to " + efoIri);
                            addedDefCitations.add(bioportalXref);
                        }
                    }
                }
            }

        }

        manager.applyChanges(changes);

        //If remove was set to true, then add the removed definition_citation to the list of def citations removed in the
        // reportLine object.
        if (remove) {
            Collection<String> removedDefCitationsDuplicate = preExistingDefCitations;


            for (String addedDefCitation : addedDefCitations) {
                preExistingDefCitations.remove(addedDefCitation);
            }

            for (String removedDefCitation : removedDefCitationsDuplicate) {
                addedDefCitations.remove(removedDefCitation);
            }


            for (String removedDefCitation : preExistingDefCitations) {
                reportLine.addRemovedId(removedDefCitation);
            }
        }

        //Add the definition citation added to the ReportLine object.
        for (String addedDefCitation : addedDefCitations) {
            reportLine.addAddedId(addedDefCitation);
        }

        if (!dummy) {
            manager.saveOntology(containerOntology);
        }


        return reportLine;
    }


    public OWLOntology getContainerOntology(OWLOntology efoOntology, OWLClass efoClass, OWLOntologyManager manager) {

        OWLOntology containerOntology = null;
        if (efoOntology.containsClassInSignature(efoClass.getIRI())) {
            containerOntology = efoOntology;
        } else {
            for (OWLOntology importedOntology : manager.getImports(efoOntology)) {
                if (importedOntology.containsClassInSignature(efoClass.getIRI()) && importedOntology.getOntologyID().getOntologyIRI().toString().equals(OntologyIRI2FilePathEnum.DISEASE_MODULE.getIri())) {
                    containerOntology = importedOntology;
                }
            }
        }
        return containerOntology;

    }

    /**
     * Get the definition_citation annotation attached to the given efoClass for the given ontology.
     *
     * @param efoClass - an efo class owl object
     * @param ontology - the ontology you're interested in (ex : OntologyEnum.HUMAN_DESEASE_ONTOLOGY for DOID)
     * @return a collection of definition_citation, can be empty but not null
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    public Collection<OWLAnnotation> getDefinitionCitation(OWLClass efoClass, OntologyEnum ontologyEnum, OWLOntology ontology, OWLDataFactory factory) throws MalformedURLException, URISyntaxException {
        URL url = new URL(ontologyEnum.getEfoDefinitionCitationIRI());
        OWLAnnotationProperty definitionCitationAnnotProperty = factory.getOWLAnnotationProperty(IRI.create(url));
        return efoClass.getAnnotations(ontology, definitionCitationAnnotProperty);
    }
}



