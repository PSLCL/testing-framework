package com.pslcl.dtf.runner.template;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToTarGz {
	
	private String tarGzFilename;
	private String sourceDirectory;

    private final Logger log;
    private final String simpleName;

	/**
	 * Add given file to given output stream, with directory nesting
	 * @param placementBase Must not be null
	 * @throws IOException 
	 * 
	 */
	private void addFileToTarGz(TarArchiveOutputStream tOut,                  // OutputStream
			                    String addPath,                               // C:\gitdtf\ws\apps\tempArtifactDirectory\
			                    String placementBase) throws IOException {    // attachments
		File f = new File(addPath);
		log.debug(simpleName + "Adds to tarGz/" + placementBase + " the file or folder " + f.getAbsolutePath());
		String placementName = placementBase;
		if (f.isFile())
			placementName += f.getName();
		TarArchiveEntry tarEntry = new TarArchiveEntry(f,              // the directory or file that the tarEntry represents
				                                       placementName); // the destination directory name, or destination directory structure, with trailing filename)
		tOut.putArchiveEntry(tarEntry);
		
		if (f.isFile()) {
			// note: if file placement name is already written to disk, this section overwrites it, without log message
			FileInputStream is = new FileInputStream(f);
			IOUtils.copy(is, tOut);
			is.close();
			tOut.closeArchiveEntry();
		} else {
			// empty attachments.tar.gz is "not a file" and comes here
			tOut.closeArchiveEntry();
			
			// add available directories or files
			File [] children = f.listFiles();
			if (children != null) {
				for (File child : children) {
					log.debug(simpleName + "Adding file or directory " + child.getName());
					this.addFileToTarGz(tOut, child.getAbsolutePath(), placementName+"/");
				}
			}
		}
	}
	
	/**
	 * 
	 * @param tarGzFilename
	 * @throws IOException
	 */
	private void CreateTarGz(String tarGzFilename, String sourceDirectory) throws IOException {
		// get absolute path of where this application resides
		//String tarGzAbsoluteFilePath = new File("").getAbsolutePath(); // does not supply the trailing '\' that we need
		String tarGzAbsoluteFilePath = new File(".").getAbsolutePath(); // add the '.' then delete it: final result has our needed trailing '\' 
		if (tarGzAbsoluteFilePath.endsWith("."))
			tarGzAbsoluteFilePath = tarGzAbsoluteFilePath.substring(0, tarGzAbsoluteFilePath.length()-1);

		// place empty file tarGzFilename.tar.gz at . 
		FileOutputStream fOut = new FileOutputStream(new File(tarGzFilename)); // if this is the same name as a previous directory or file, then, when this is eventually written, it will blow away the previous
		log.debug(simpleName + "created new file " + tarGzFilename + " at directory " + tarGzAbsoluteFilePath);
		
		// add to file tarGzFilename.tar.gz whatever directories and files exist at sourceDirectory, with full directory nesting  
		BufferedOutputStream bOut = new BufferedOutputStream(fOut);
		GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
		TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);
		try {
			String addPath = tarGzAbsoluteFilePath + sourceDirectory;
			this.addFileToTarGz(tOut,                                // empty OutputStream
					            addPath,                             // C:\gitdtf\ws\apps\tempArtifactDirectory\
					            InspectHandler.archiveTopDirectory); // attachments 
		} finally {
			if (tOut != null) {
				tOut.finish(); // write file-populated tOut to disk
				tOut.close(); // also closes gzOut, bOut, fOut
			} else if (gzOut != null) {
				gzOut.close(); // also closes bOut, fOut
			} else if (bOut != null) {
				bOut.close(); // also closes fOut
			} else if (fOut != null) {
				fOut.close();
			}
		}
	}

	/**
	 * Constructor
	 * @param tarGzFilename name of tarGz file to create, e.g. attachments.tar.gz
	 */
	ToTarGz(String tarGzFilename, String sourceDirectory) {
		this.tarGzFilename = tarGzFilename;
		this.sourceDirectory = sourceDirectory;
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
	}
	
	/**
	 * 
	 * @throws IOException 
	 */
	void CreateTarGz() throws IOException {
		CreateTarGz(this.tarGzFilename, this.sourceDirectory); // this.tarGzFilename: attachments; this.sourceDirectory: tempArtifactDirectory  
	}
	
	/**
	 * 
	 * @return An input stream that holds a tar archive
	 * @throws IOException
	 */
	TarArchiveInputStream getTarArchiveInputStream() throws IOException {
		// Based on example 3: http://www.programcreek.com/java-api-examples/index.php?api=org.apache.commons.compress.archivers.tar.TarArchiveInputStream . Many other examples are shown there to handle many use cases.
		
//		BufferedInputStream bufIS = new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(this.tarGzFilename))));
		File tarGz = new File(this.tarGzFilename);
		FileInputStream fIS = new FileInputStream(tarGz);
		GZIPInputStream gzIS = new GZIPInputStream(fIS);
		BufferedInputStream bufIS = new BufferedInputStream(gzIS);
		
		try {
			TarArchiveInputStream retTAIS = new TarArchiveInputStream(bufIS);
			if (true) { // true: temporarily, use this independently created TAIS as a way to count how many tar entries are present, and to report their names
				// replicate the above without using retTAIS, which must remain pristine
				TarArchiveInputStream testTAIS = new TarArchiveInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(new File(this.tarGzFilename)))));
				TarArchiveEntry taEntry;
				List<TarArchiveEntry> tarArchiveEntries = new ArrayList<TarArchiveEntry>();
				while ((taEntry = testTAIS.getNextTarEntry()) != null) {
					log.debug(this.simpleName + "tais entry found: " + taEntry.getName());
					tarArchiveEntries.add(taEntry);
				}
				log.debug(simpleName + tarArchiveEntries.size() + " TarArchiveEntry's found"); // I see count of 1 for top directory "attachments," another for file1, another for file2
				testTAIS.close();
			}
			return retTAIS; // TODO: close this InputStream at the right time; cannot close it putStream until it is used by PersonProvider
		} catch (Exception e) {
			log.debug(simpleName + "getTarArchiveInputStream() sees exception : " + e.getMessage());
			bufIS.close();
			throw e;
		}
	}
	
	/**
	 * use as a unit test
	 * @param tais
	 */
	void writeFileStructure(TarArchiveInputStream tais) throws IOException {
		// delete the entire top level destination directory
		String destTopDir = "recoveryDirectory";
		File destTopPath = new File(destTopDir);
        FileUtils.deleteDirectory(destTopPath); // Whether directory is present, or not, this operates without exception. note: windows file explorer cannot be into this directory
        
		TarArchiveEntry taEntry;
		while ((taEntry = tais.getNextTarEntry()) != null) {
			File destPath = new File(destTopDir, taEntry.getName()); // appends a path of directories to the top directory
			log.debug(this.simpleName + "writeFileStructure() forming taEntry for writing to disk: " + destPath.getCanonicalPath());
			if (taEntry.isDirectory()) {
				destPath.mkdirs();
			} else {
				destPath.createNewFile(); // empty file
				long fileSize = taEntry.getSize();
				byte [] readBuf = new byte[(int)fileSize];
				FileOutputStream fos = new FileOutputStream(destPath);
				BufferedOutputStream bfos = new BufferedOutputStream(fos);
				while (true) {
					int len = tais.read(readBuf); // this read is defined to occur only on the taEntry received by the last tais.getNextTarEntry() call
					if (len == -1)
						break;
					bfos.write(readBuf, 0, len);
					log.debug(this.simpleName + "writeFileStructure() wrote " + len + " bytes to file " + destPath.getCanonicalPath());
				}
				bfos.close();
			}
		} // end while()
	}

	/**
	 * use as a unit test
	 * @param inputStream
	 * @param dummy
	 * @throws IOException
	 */
	void writeFileStructure(InputStream inputStream, int dummy) throws IOException {
//		GZIPInputStream gzIS = new GZIPInputStream(inputStream); // EOF exception
//		BufferedInputStream bufIS = new BufferedInputStream(gzIS);
//		TarArchiveInputStream tais = new TarArchiveInputStream(bufIS);
		TarArchiveInputStream tais = (TarArchiveInputStream)inputStream; // this is downcasting: would fail at run time, but only if param inputStream was NOT REALLY a TarArchiveInputStream
		this.writeFileStructure(tais);
	}
	
}