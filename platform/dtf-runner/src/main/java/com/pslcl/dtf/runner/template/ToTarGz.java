package com.pslcl.dtf.runner.template;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToTarGz {

    private String tarGzipFullPathFilename;
    private String sourceDirectory;

    private final Logger log;
    private final String simpleName;

    /**
     * Write a tarGzip file to disk, with directory and file entries from a given source directory
     *
     * @param tarGzipFilename
     * @return
     * @throws IOException
     */
    private File CreateTarGz(String tarGzipFullPathFilename, String sourceDirectory) throws IOException {
        // tar and compress sourceDirectory to a tarGzip file, by
        // creating a tarGz of InspectHandler.archiveFilename, specified with its full path; the output file has filename InspectHandler.archiveFilename, but is placed at the fully specified path
        File sourceFile = new File(sourceDirectory);
        File retFileTarGzip = new File(tarGzipFullPathFilename);
        this.tarCompressFile(sourceFile, retFileTarGzip);
        return retFileTarGzip;
    }

    /**
     * Tar and compress one submitted directory or file.
     *
     * @param singleFile The one directory or file to tar and compress
     * @param tarGzipOut The output tarGzip file
     * @throws IOException
     */
    private void tarCompressFile(File singleFile, File tarGzipOut) throws IOException {
        List<File> listOneFile = new ArrayList<File>(1); // convert file to one entry list of files
        listOneFile.add(singleFile);
        this.tarGzipCompress(listOneFile, tarGzipOut);
    }

    /**
     * Tar and compress submitted list of directories and files
     *
     * @param files The list of directories and files to tar and compress
     * @param tarGzipOut The output tarGzip file
     * @throws IOException
     */
    private void tarGzipCompress(Collection<File> files, File tarGzipOut) throws IOException {
        FileOutputStream fos = null;
        GZIPOutputStream gzipOS = null;
        TarArchiveOutputStream taos = null;
        try {
            // add to file tarGzipOut whatever directories and files exist in param files, with full directory nesting
            log.trace(this.simpleName + "tarGzipCompress() adds " + files.size() + " directory or file to " + tarGzipOut.getName() + ", placed at " + tarGzipOut.getAbsolutePath());
            fos = new FileOutputStream(tarGzipOut); // if this is the same name as a previous directory or file, then, when this is eventually written, it will blow away the previous
            gzipOS = new GZIPOutputStream(fos); // wrap fos gzip behavior
            taos = new TarArchiveOutputStream(gzipOS); // wrap fos with tar behavior

            // put each file in taos
            for (File file : files) {
                this.addFileToTaos(taos, file, ".");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (taos != null) {
                taos.finish(); // ends the archive without closing the underlying OutputStream; can probably use close() alone
                taos.close();  // also closes gzipOS and fos
            } else if (gzipOS != null) {
                gzipOS.close(); // also closes fos
            } else if (fos != null) {
                fos.close();
            }
        }

        // temporarily as a test, write tarGzipOut's directories and files to disk, at a temporary directory recovered_xxx
        //    (if class TestUtility is placed in src/test/java, arrange that project path's output Folder to be target/classes instead of target/test-classes
//        FileInputStream fis = new FileInputStream(tarGzipOut);
//        String destTopDirectory = "recovered_" + this.sourceDirectory;
//        TestUtility.writeFileStructure(fis, destTopDirectory);
//        fis.close();
    }

    /**
     * Add a file or directory to the TarArchiveOutputStream
     *
     * Note: Params must not be null
     * @param taos The tar archive output stream to hold the submitted directory or file
     * @param addFile the directory or file to add
     * @param parentDir Within taos, the name of the directory to hold the submitted directory or file, such as "."
     * @throws IOException
     */
    private void addFileToTaos(TarArchiveOutputStream taos, File addFile, String parentDir) throws IOException {
        String entryName;
        if (parentDir.equals("."))
            entryName = InspectHandler.archiveTopDirectory; // "attachments": does not exist anywhere in actuality, but in taos, place this as the reference "top" directory
        else
            entryName = parentDir + File.separator + addFile.getName();
        log.trace(this.simpleName + "addFileToTaos() sees parentDir '" + parentDir + "'; submits file " + addFile + ", of entryName " + entryName);
        TarArchiveEntry taEntry = new TarArchiveEntry(addFile, entryName);
        taos.putArchiveEntry(taEntry);
        if (addFile.isFile()) {
            // copy addFile to taos
            log.trace(this.simpleName + "addFileToTaos() adds file " + addFile + " under directory " + parentDir);
            FileInputStream addFIS = new FileInputStream(addFile);
            BufferedInputStream bufIS = new BufferedInputStream(addFIS);
            IOUtils.copy(bufIS, taos);
            taos.closeArchiveEntry();
            bufIS.close();
        } else {
            // our newly formed directory taEntry was added to taos, but is now complete
            taos.closeArchiveEntry();

            // recursively add child files (or directories) to the parent entry that was passed in as params
            String parentName = entryName;
            File [] childFiles = addFile.listFiles();
            for (File childFile : childFiles) {
                log.trace(this.simpleName + "addFileToTaos() recursively adds childFile " + childFile + " under directory " + parentName);
                this.addFileToTaos(taos, childFile, parentName);
            }
        }
    }

    /**
     * Constructor
     * @param tarGzipFullPathFilename name of tarGzip file to create, e.g. attachments.tar.gzip
     */
    ToTarGz(String tarGzipFullPathFilename, String sourceDirectory) {
        this.tarGzipFullPathFilename = tarGzipFullPathFilename;
        this.sourceDirectory = sourceDirectory;
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
    }

    /**
     *
     * @throws IOException
     */
    File CreateTarGz() throws IOException {
        return this.CreateTarGz(this.tarGzipFullPathFilename, this.sourceDirectory);
    }

    /**
     * From the given tarGz File, return its FileInputStream.
     *
     * @param fileTarGz
     * @return
     * @throws FileNotFoundException
     */
    FileInputStream getFileInputStream(File fileTarGz) throws FileNotFoundException {
        return new FileInputStream(fileTarGz);
    }

    /**
     * From the given tarGz File, return its TarArchiveInputStream.
     * @param fileTarGz
     * @return An input stream that holds a tar archive
     * @throws IOException
     */
    TarArchiveInputStream getTarArchiveInputStream(File fileTarGz) throws IOException {
        // this is an alternative that can be used

        // Based on example 3: http://www.programcreek.com/java-api-examples/index.php?api=org.apache.commons.compress.archivers.tar.TarArchiveInputStream . Many other examples are shown there to handle many use cases.

//        BufferedInputStream bufIS = new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(this.tarGzFilename))));
        FileInputStream fIS = new FileInputStream(fileTarGz);
        GZIPInputStream gzIS = new GZIPInputStream(fIS);
        BufferedInputStream bufIS = new BufferedInputStream(gzIS);

        try {
            TarArchiveInputStream retTAIS = new TarArchiveInputStream(bufIS);

            // temporarily as a test, use this independently created TAIS as a way to count how many tar entries are present, and to report their names; allows retTAIS to remain pristine, as it must
//            TarArchiveInputStream testTAIS = new TarArchiveInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(this.tarGzipFilename)))));
//            TarArchiveEntry taEntry;
//            List<TarArchiveEntry> tarArchiveEntries = new ArrayList<TarArchiveEntry>();
//            while ((taEntry = testTAIS.getNextTarEntry()) != null) {
//                log.debug(this.simpleName + "tais entry found: " + taEntry.getName());
//                tarArchiveEntries.add(taEntry);
//            }
//            log.debug(simpleName + tarArchiveEntries.size() + " TarArchiveEntry's found");
//            testTAIS.close();

            // temporarily as a test, use this independently created TAIS to write the archive's directories and files to disk, at a temporary directory recovered_xxx
            //    (if class TestUtility is placed in src/test/java, arrange that project path's output Folder to be target/classes instead of target/test-classes
//            TarArchiveInputStream testTAIS = new TarArchiveInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(this.tarGzipFilename)))));
//            String destTopDirectory = "recovered_" + this.sourceDirectory;
//            TestUtility.writeFileStructure(testTAIS, destTopDirectory);
//            testTAIS.close();

            return retTAIS;
        } catch (Exception e) {
            log.debug(simpleName + "getTarArchiveInputStream() sees exception : " + e.getMessage());
            bufIS.close();
            throw e;
        }
    }

}