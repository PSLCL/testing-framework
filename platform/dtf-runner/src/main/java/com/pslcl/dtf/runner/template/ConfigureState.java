package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;

public class ConfigureState {
	static private boolean allProgramsRan = false;
	
	static public void setAllProgramsRan(boolean allProgramsRan) {
		ConfigureState.allProgramsRan = allProgramsRan;
	}
	
	static public boolean getAllProgramsRan() {
		return allProgramsRan;
	}
	
	private Future<Integer> futureProgramRunResult = null;
	private MachineInstance machineInstance;
	private Integer programRunResult = null;
	
	public ConfigureState(Future<Integer> futureProgramRunResult, MachineInstance machineInstance) {
		this.futureProgramRunResult = futureProgramRunResult;
		this.machineInstance = machineInstance;
	}

	Future<Integer> getFutureProgramRunResult() {
		return this.futureProgramRunResult;
	}
	
	MachineInstance getMachineInstance() {
		return this.machineInstance;
	}
	
	void setProgramRunResult(Integer programRunResult) {
		this.programRunResult = programRunResult;
	}
	
	Integer getProgramRunResult() {
		return this.programRunResult;
	}

}