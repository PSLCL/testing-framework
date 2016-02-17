package com.pslcl.dtf.runner.template;

import com.pslcl.dtf.runner.process.DBTemplate;

/**
 * 
 * @author cjones
 *
 */
public class IncludeInfo {
	private String templateHash;
	private DBTemplate dbTemplate;
	
	/**
	 * Constructor
	 * 
	 * @param templateHash
	 */
	IncludeInfo(String templateHash) {
		this.templateHash = templateHash;
		this.dbTemplate = null;
	}
	
	/**
	 * 
	 * @param dbTemplate
	 */
	void setDBTemplate(DBTemplate dbTemplate) {
		this.dbTemplate = dbTemplate;
	}
	DBTemplate getDBTemplate() {
		return this.dbTemplate;
	}
	
	/**
	 * 
	 * @return
	 */
	String getTemplateHash() {
		return this.templateHash;
	}
}
