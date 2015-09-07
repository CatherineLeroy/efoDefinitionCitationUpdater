package uk.ac.ebi.spot.updater;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import uk.ac.ebi.spot.updater.utils.OntologyIRI2FilePathEnum;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by catherineleroy on 26/08/2015.
 */
public class XrefGetterCaller {

    static Logger LOG = Logger.getLogger(XrefGetterCaller.class.getName());

    public static void main(String[] args) throws OWLOntologyCreationException, ExecutionException, InterruptedException, FileNotFoundException {

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        XrefGetterCaller xrefGetterCaller = new XrefGetterCaller();

        /**
         * The directory containing the efo_release_candidate.owl and all the owl file it imports.
         */
        String ontologyDirectory = null;

        /**
         * The node for which the script will get all the children and search them into bioportal to get their ontology
         * xref.
         * ex : EFO_0000408 for disease, EFO_0005297 for phenotype ...etc
         */
        String parentClassEfoId = null;

        //-parent EFO_0005297 -output /Users/catherineleroy/Documents/non_github_project/EFOxrefUpdate/xrefGetterCaller_ouput.txt -ontoDir /Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate

        String output = null;
        for(int i = 0; i<args.length; i++){


            if("-ontoDir".equals(args[i])){
                ontologyDirectory = args[i+1];
                i++;

            }
            if("-parent".equals(args[i])){
                parentClassEfoId = args[i+1];
                i++;

            }

            if("-output".equals(args[i])){
                output = args[i+1];
                i++;

            }

        }
        if(ontologyDirectory == null){
            throw new IllegalArgumentException("You must provide parameter -ontoDir (The directory containing the efo_release_candidate.owl and all the owl file it imports) ");
        }

        if(output == null){
            throw new IllegalArgumentException("You must provide parameter -output (The path to the output file) ");
        }


        if(!ontologyDirectory.endsWith("/")){
            ontologyDirectory = ontologyDirectory + "/";
        }

        // If parentClassEfoId wasn't provided then set it to the parent of all term EFO_0000001.
        if(parentClassEfoId == null){
            parentClassEfoId = "EFO_0000001";
        }

//        String ontologyFileDirectory = "/Users/catherineleroy/Documents/github_project/ExperimentalFactorOntology/ExFactorInOWL/releasecandidate/";
        String ontologyFileName = "efo_release_candidate.owl";

//        Collection<OWLClass> allClasses =  xrefGetterCaller.getAllChildren("EFO_0000408", ontologyFileDirectory, ontologyFileName);
//        Phenotype
//        Collection<OWLClass> allClasses =  xrefGetterCaller.getAllChildren("EFO_0000001", ontologyDirectory, ontologyFileName);
        Collection<OWLClass> allClasses =  xrefGetterCaller.getAllChildren(parentClassEfoId, ontologyDirectory, ontologyFileName);
//        Collection<OWLClass> allClasses =  xrefGetterCaller.getAllChildren("EFO_0005297", ontologyFileDirectory, ontologyFileName);
        LOG.info("Going to update " + allClasses.size() + " classes. ");

        List<Callable<String>> callables = new ArrayList<>();
        int totalClassUpdated = 0;
        for (OWLClass class2update : allClasses) {

            if(totalClassUpdated < 100) {
                //Do not update owl:Nothing as there is nothing to update there and do not update anything that comes from
                //orphanit as this is updated every month from orphanet.
                if (!class2update.toString().contains("www.orpha.net") && !class2update.toString().equals("owl:Nothing")) {
//                    totalClassUpdated++;
                    //Create a runnable to update the efo class.
                    Callable<String> callable = new XrefGetterCallable(class2update, ontologyDirectory, ontologyFileName);
                    callables.add(callable);

                }
            }
        }


        //Output result to output file.
        List<Future<String>> futures = executorService.invokeAll(callables);
        PrintWriter writer = new PrintWriter(output);
        for (Future<String> future : futures) {
            System.out.println(future.get());
            writer.println(future.get());
        }
        writer.close();


        executorService.shutdown();

        try {
            executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();

    }

    /**
     * Returns a Collection of children for the given owl class IRI in the given ontology.
     * @param efoId - the efo id for which you want to get the children (ex : EFO_0000408).
     * @return an OWLClass collection of the children to which we add the given parent term. So, the smallest collection
     * returned should contain at least one owlClass (the parent), unless the given efo id is not found.
     * @throws OWLOntologyCreationException
     */
    public Collection<OWLClass> getAllChildren(String efoId, String ontologyFileDirectory, String ontologyFileName) throws OWLOntologyCreationException {

        File efoFile = new File(ontologyFileDirectory + ontologyFileName);

        //Import all owl files (efo_disease_module.owl, efo_ordo_module.owl ...).
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        for(OntologyIRI2FilePathEnum enumeration : OntologyIRI2FilePathEnum.values()){
            manager.addIRIMapper(
                    new SimpleIRIMapper( IRI.create(enumeration.getIri()),
                            IRI.create( "file:" + ontologyFileDirectory + enumeration.getFileName()  ) ) );
        }


        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLOntologyDocumentSource owlOntologyDocumentSource = new FileDocumentSource(efoFile);
        OWLOntology efo = manager.loadOntologyFromOntologyDocument(owlOntologyDocumentSource,config);

        PrefixManager efoPrefixManager = new DefaultPrefixManager("http://www.ebi.ac.uk/efo/");

        // GET ALL THE CLASSES TO UPDATE
        // ------------------------------
        // I use the reasonner to get all the children of the root class (EFO_0000001 to get all the classes of the efo
        // ontology).
        // I then make sure that
        //
        //disease
//        OWLClass root = factory.getOWLClass("EFO_0000408",efoPrefixManager);
        //root
        OWLClass root = factory.getOWLClass(efoId,efoPrefixManager);


        // Use the reasonner to get all the children of the disease OWLClass in a set.
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration simpleConfig = new SimpleConfiguration(progressMonitor);
        OWLReasoner reasoner = reasonerFactory.createReasoner(efo, simpleConfig);
        reasoner.precomputeInferences();

        NodeSet<OWLClass> subClassesNode = reasoner.getSubClasses(root, false);
        Set<OWLClass> allClasses = subClassesNode.getFlattened();
        //Adding the root to the allClasses Collection. This is not very usefull in case the root is EFO_0000001, as this
        //won't have any xref but is usefull if the root is neoplasm for example.
        allClasses.add(root);
        LOG.info("Going to update " + allClasses.size() + " classes. ");

        return allClasses;
    }
}
