package com.pslcl.dtf.runner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

public class TestUtility {

    /**
     * Write a tar/gzip-based FileInputStream to disk.
     * @param fis
     * @throws IOException
     */
    static public void writeFileStructure(FileInputStream fis, String destTopDirectory) throws IOException {
        TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(fis));
        TestUtility.writeFileStructure(tais, destTopDirectory);
    }

    /**
     * Write a TarArchiveinputStream to disk.
     * @param tais
     * @throws IOException
     */
    static public void writeFileStructure(TarArchiveInputStream tais, String destTopDirectory) throws IOException {
        // delete the entire top level destination directory
        File destTopPath = new File(destTopDirectory);
        FileUtils.deleteDirectory(destTopPath); // no exception for directory not present

        TarArchiveEntry taEntry;
        while ((taEntry = tais.getNextTarEntry()) != null) {
            File newFile = new File(destTopDirectory, taEntry.getName()); // appends a path of directories to the top directory
            LoggerFactory.getLogger("TestUtility").debug(" writeFileStructure() forming taEntry for writing to disk: " + newFile.getCanonicalPath());
            if (taEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                newFile.createNewFile(); // empty file
                long fileSize = taEntry.getSize();
                byte [] readBuf = new byte[(int)fileSize];
                FileOutputStream fos = new FileOutputStream(newFile);
                BufferedOutputStream bfos = new BufferedOutputStream(fos);
                while (true) {
                    int len = tais.read(readBuf); // this read is defined to occur only on the taEntry received by the last tais.getNextTarEntry() call
                    if (len == -1)
                        break;
                    bfos.write(readBuf, 0, len);
                    LoggerFactory.getLogger("TestUtility").debug(" writeFileStructure() wrote " + len + " bytes to file " + newFile.getCanonicalPath());
                }
                bfos.close();
            }
        } // end while()
    }

}