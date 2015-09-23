package uk.ac.ebi.spot.updater;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.*;
import java.net.URL;

/**
 * Created by catherineleroy on 23/09/2015.
 */
public class EfoXrefUpdaterTests {


    @Test
    public void updateTest() throws Exception {

        URL url = this.getClass().getResource("/efoXrefUpdaterFiles");
        File testFilesDirectory = new File(url.getFile());
        System.out.println(testFilesDirectory.getAbsolutePath());

        File testFilesDirectoryCopy = new File(testFilesDirectory.getAbsolutePath() + "_copy");

        copyFolder(testFilesDirectory, testFilesDirectoryCopy);

        EfoXrefUpdater efoXrefUpdater = new EfoXrefUpdater();
        String options = "-dummy true -remove true" +
                        " -ontoDir " + testFilesDirectory.getAbsolutePath() +
                        " -i " + testFilesDirectory.getAbsolutePath() + "/xrefUpdater_input.txt" +
                        " -output " + testFilesDirectory.getAbsolutePath() + "/output.txt";
        String args[] = options.split(" ");
        efoXrefUpdater.getOptions(args);
        efoXrefUpdater.update();

        File outputFile = new File(testFilesDirectory.getAbsolutePath() + "/output.txt");


        //EFO class IRI	EFO class label	Removed Def Citation	Added Def Citation
//        http://www.ebi.ac.uk/efo/EFO_0002939	medulloblastoma	DOID:0060105	NCIt:C3222,DOID:0050902,MSH:D008527,SNOMEDCT:83217000,SNOMEDCT:443333004
//        http://purl.obolibrary.org/obo/UBERON_0001137	dorsum	NCI_Thesaurus:Back	NCIt:C32481,MSH:D001415

        String currentLine;

        BufferedReader br = new BufferedReader(new FileReader(outputFile.getAbsolutePath()));

        int i = 0;
        while ((currentLine = br.readLine()) != null) {
            if(i == 0){
                assertEquals(currentLine, "EFO class IRI\tEFO class label\tRemoved Def Citation\tAdded Def Citation\t");
            }
            if(i == 1){
                assertEquals(currentLine, "http://www.ebi.ac.uk/efo/EFO_0002939\tmedulloblastoma\tDOID:0060105\tNCIt:C3222,DOID:0050902,MSH:D008527,SNOMEDCT:83217000,SNOMEDCT:443333004");
            }
            if(i == 2){
                assertEquals(currentLine, "http://purl.obolibrary.org/obo/UBERON_0001137\tdorsum\tNCI_Thesaurus:Back,SNOMEDCT:123961009\tNCIt:C32481,MSH:D001415");
            }
            System.out.println(currentLine);
            i++;
        }



    }



    public static void copyFolder(File src, File dest) throws IOException {

        if(src.isDirectory()){

            //if directory not exists, create it
            if(!dest.exists()){
                dest.mkdir();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile,destFile);
            }

        }else{
            //if file, then copy it
            //Use bytes stream to support all file types
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
            System.out.println("File copied from " + src + " to " + dest);
        }
    }






}
