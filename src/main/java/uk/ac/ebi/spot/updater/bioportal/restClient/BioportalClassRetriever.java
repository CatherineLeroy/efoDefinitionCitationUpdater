package uk.ac.ebi.spot.updater.bioportal.restClient;

import uk.ac.ebi.spot.updater.bioportal.exceptions.BioportalDeadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.spot.updater.utils.OntologyEnum;

/**
 * Created by catherineleroy on 20/01/2015.
 */
public class BioportalClassRetriever {

    //ex : http://data.bioontology.org/search?q=cancer&ontologies=MESH&require_exact_match=true
    private String urlPattern = "http://data.bioontology.org/search?q=LABEL&ontologies=ONTOLOGY&require_exact_match=EXACT_MATCH_BOOLEAN&display_links=false";

    private String parentUrlPattern = "http://data.bioontology.org/ontologies/NCIT/classes/http%3A%2F%2Fncicb.nci.nih.gov%2Fxml%2Fowl%2FEVS%2FThesaurus.owl%23ID_TO_REPLACE/parents";
//    private String parentUrlPattern = "data.bioontology.org/ontologies/NCIT/classes/http%3A%2F%2Fncicb.nci.nih.gov%2Fxml%2Fowl%2FEVS%2FThesaurus.owl%23ID_TO_REPLACE/parents";

    private static final String API_KEY = "12e2ffa9-0c8b-4f3d-a0d2-8edaadc0378f";

    static final ObjectMapper mapper = new ObjectMapper();

    private String urlString;

    public  JsonNode getJsonNode(String label, OntologyEnum ontologyEnum, boolean isExactMatch) throws InterruptedException, BioportalDeadException {

        JsonNode bioportalNode = null;
        String result = "";
        //replace in the label '%' by it's url code '%25' and then replace any space character by %20 for the url to work
        // ex : http://data.bioontology.org/search?q=Chagas cardiomyopathy&ontologies=DOID&require_exact_match=true
        // becomes
        //      http://data.bioontology.org/search?q=Chagas%20cardiomyopathy&ontologies=DOID&require_exact_match=true
        //It is important to replace the % before the space as otherwise the %20 replacing a space will become %2520.
        label = label.replace("%", "%25");
        label = label.replace(" ", "%20");
        label = label.replace("'","%27");

        try{
            result = get(label,ontologyEnum,isExactMatch);
            bioportalNode = BioportalClassRetriever.jsonToNode(result, false);
        }catch(IOException exception){
            try{
                //Wait 3 seconds and see if bioportal has recover
                Thread.sleep(5000);
                result = get(label,ontologyEnum,isExactMatch);
                bioportalNode = BioportalClassRetriever.jsonToNode(result, false);
            }catch(IOException exceptionBis){
                try{
//                    System.out.println(this.urlString);
                    //Wait 3 seconds and see if bioportal has recover
                    Thread.sleep(5000);
                    result = get(label,ontologyEnum,isExactMatch);
                    bioportalNode = BioportalClassRetriever.jsonToNode(result, false);
                }catch(IOException exceptionTris){
                    throw new BioportalDeadException(exceptionTris.getMessage() + " \nLabel =" + label + "= OntologyEnum ="+ ontologyEnum.getBioPortalAcronym() + "= Url was : "  + this.urlString);
                }
            }
        }
        return bioportalNode;
    }

    public  JsonNode getJsonNodeWithClassId(String classId, OntologyEnum ontologyEnum) throws InterruptedException, BioportalDeadException {

        JsonNode bioportalNode = null;
        String result = null;
        try{
            result = getWithClassId(classId, ontologyEnum);
            if(result == null){
                bioportalNode = null;
            }else {
                bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
            }
        }catch(IOException exception){
            try{
                //Wait 5 minutes and see if bioportal has recover
                Thread.sleep(5000);
                result = getWithClassId(classId, ontologyEnum);
                if(result == null){
                    bioportalNode = null;
                }else {
                    bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
                }
            }catch(IOException exceptionBis){
                try{
                    //Wait 5 minutes and see if bioportal has recover
                    Thread.sleep(5000);
                    result = getWithClassId(classId, ontologyEnum);
                    if(result == null){
                        bioportalNode = null;
                    }else {
                        bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
                    }
                }catch(IOException exceptionTris){
                    throw new BioportalDeadException(exceptionTris.getMessage());
                }
            }
        }
        return bioportalNode;
    }

    public JsonNode getParents(String id, OntologyEnum ontologyEnum) throws InterruptedException, BioportalDeadException {
        JsonNode bioportalNode = null;
        if(!ontologyEnum.equals(OntologyEnum.NCI_THESAURUS)){
            throw new RuntimeException("method not implemented for ontology " + ontologyEnum);
        }

        String url = parentUrlPattern.replace("ID_TO_REPLACE", id);
        String result = null;
        try{
            result = getContentFromUrl(url);
            if(result == null){
                bioportalNode = null;
            }else {
                bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
            }
        }catch(IOException exception){
            try{
                //Wait 5 minutes and see if bioportal has recover
                Thread.sleep(5000);
                result = getContentFromUrl(url);
                if(result == null){

                    bioportalNode = null;
                }else {
                    bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
                }
            }catch(IOException exceptionBis){

                try{
                    //Wait 5 minutes and see if bioportal has recover
                    Thread.sleep(5000);
                    result = getContentFromUrl(url);
                    if(result == null){

                        bioportalNode = null;
                    }else {
                        bioportalNode = BioportalClassRetriever.jsonToNode(result, true);
                    }
                }catch(IOException exceptionTris){

                    throw new BioportalDeadException(exceptionTris.getMessage());
                }
            }
        }

        return bioportalNode;
    }


    public String getContentFromUrl(String urlString) throws IOException {

        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        URL url = new URL(urlString);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);

        conn.setRequestProperty("Accept", "application/json");


        boolean isError = conn.getResponseCode() >= 400;
        if(isError) {
            String errorMessage = conn.getResponseMessage();

            if(errorMessage.equals("Not Found")){
                return null;
            }

        }
        rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();

        //System.out.println("result = " + result);
        //System.out.println("stop");
        return result;

    }


    public String getWithClassId(String classId,OntologyEnum ontologyEnum)throws IOException {
        classId = classId.replaceAll(":","%3A");
        classId = classId.replaceAll("/","%2F");
        classId = classId.replaceAll("#","%23");


        urlString = "http://data.bioontology.org/ontologies/ONTOLOGIE_BIOPORTAL_ACRONYM/classes/CLASS_ID";
        urlString = urlString.replace("ONTOLOGIE_BIOPORTAL_ACRONYM", ontologyEnum.getBioPortalAcronym());
        urlString = urlString.replace("CLASS_ID", classId);


//        System.out.println("urlString = " + urlString);

        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        URL url = new URL(urlString);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        boolean isError = conn.getResponseCode() >= 400;
        if(isError) {
            String errorMessage = conn.getResponseMessage();
            if(errorMessage.equals("Not Found")){
                return null;
            }

        }

            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();

        //System.out.println("result = " + result);
        //System.out.println("stop");
        return result;
    }



    public  String get(String label, OntologyEnum ontologyEnum, boolean isExactMatch) throws IOException {
        URL url;
        urlString = urlPattern.replaceAll("LABEL", label).replaceAll("ONTOLOGY",ontologyEnum.getBioPortalAcronym()).replaceAll("EXACT_MATCH_BOOLEAN",Boolean.toString(isExactMatch));

//        System.out.println(this.urlString);

        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        url = new URL(urlString);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();

        return result;

    }





    public static JsonNode jsonToNode(String json, boolean isClass) throws IOException {
        JsonNode root = null;

            root = mapper.readTree(json);

            if(isClass == false) {
                if ("0".equals(root.get("pageCount").asText())) {
                    // System.out.println("\tpageCound = 0");
                    return null;
                } else {
                    boolean matchTypeIsPrefLabelForAtLeastOneOfTheMatch = false;
                    for (JsonNode collection : root.get("collection")) {
                        //System.out.println("1 : MatchType is = " + collection.get("matchType").asText());
                        if ("prefLabel".equals(collection.get("matchType").asText()) || "synonym".equals(collection.get("matchType").asText())) {
                            matchTypeIsPrefLabelForAtLeastOneOfTheMatch = true;
                        }
                    }
                    if (matchTypeIsPrefLabelForAtLeastOneOfTheMatch == false) {
                        return null;
                    }
                }
            }else{
                 if(root.get("errors") != null){
                     return null;
                 }
            }



        return root;
    }

//    public static void main(String[] args) throws BioportalDeadException, InterruptedException, IOException {
//        System.setProperty("java.net.useSystemProxies", "true");
//
//        BioportalClassRetriever bioportalClassRetriever = new BioportalClassRetriever();
//        JsonNode jsonNode = bioportalClassRetriever.getJsonNodeWithClassId("http://purl.bioontology.org/ontology/ICD9CM/207.00", OntologyEnum.INTERNATIONAL_CLASSIFICATION_OF_DISEASES);
////        if(jsonNode == null){
////            System.out.println("json is null");
////        }else{
////            System.out.println("json is NOT null");
////        }
//        jsonNode = bioportalClassRetriever.getJsonNodeWithClassId("http://purl.bioontology.org/ontology/ICD9CM/207.123456", OntologyEnum.INTERNATIONAL_CLASSIFICATION_OF_DISEASES);
////        if(jsonNode == null){
////            System.out.println("json is null");
////        }else{
////            System.out.println("json is NOT null");
////        }
//
//    }

    public static void main(String[] args) throws IOException {
        URL oracle = new URL("http://data.bioontology.org/ontologies/NCIT/classes/C27878");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(oracle.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            System.out.println(inputLine);
        in.close();
    }

}
