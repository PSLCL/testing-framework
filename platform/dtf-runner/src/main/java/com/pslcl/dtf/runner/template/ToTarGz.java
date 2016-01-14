package com.pslcl.dtf.runner.template;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToTarGz {
	
	private String sourceDirectory;
	private String tarGzFilename;

    private final Logger log;
    private final String simpleName;

	/**
	 * 
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
	 * @param tarGzFilename name of tarGz file to create, e.g. attachments.tar.gz
	 */
	ToTarGz(String sourceDirectory, String tarGzFilename) {
		this.sourceDirectory = sourceDirectory;
		this.tarGzFilename = tarGzFilename;
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
	}

	/**
	 * @throws FileNotFoundException, IOException 
	 * 
	 */
	void CreateTarGz() throws FileNotFoundException, IOException {
		String tarGzAbsoluteFilePath = new File(".").getAbsolutePath();
		if (tarGzAbsoluteFilePath.endsWith("."))
			tarGzAbsoluteFilePath = tarGzAbsoluteFilePath.substring(0, tarGzAbsoluteFilePath.length()-1);
			
		FileOutputStream fOut = new FileOutputStream(new File(this.tarGzFilename)); // empty file written
		log.debug(simpleName + "created new file " + this.tarGzFilename + " at directory " + tarGzAbsoluteFilePath);
		BufferedOutputStream bOut = new BufferedOutputStream(fOut);
		GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
		TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);
		try {
			String addPath = tarGzAbsoluteFilePath + this.sourceDirectory;
			this.addFileToTarGz(tOut,                                // empty OutputStream
					            addPath,                             // C:\gitdtf\ws\apps\tempArtifactDirectory\
					            InspectHandler.archiveTopDirectory); // attachments 
		} finally {
			if (tOut != null) {
				tOut.finish();
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
	
}