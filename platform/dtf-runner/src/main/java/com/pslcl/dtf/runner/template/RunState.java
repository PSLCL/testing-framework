package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;

public class RunState {
	static private boolean allProgramsRan = false;
	
	static public void setAllProgramsRan(boolean allProgramsRan) {
		RunState.allProgramsRan = allProgramsRan;
	}
	
	static public boolean getAllProgramsRan() {
		return allProgramsRan;
	}
	
	private Future<RunnableProgram> futureRunnableProgram;
	private MachineInstance machineInstance;
	private Integer programRunResult = null;
	private RunnableProgram runnableProgram = null;
	
	public RunState(Future<RunnableProgram> futureRunnableProgram, MachineInstance machineInstance) {
		this.futureRunnableProgram = futureRunnableProgram;
		this.machineInstance = machineInstance;
	}
	
	Future<RunnableProgram> getFutureRunnableProgram() {
		return this.futureRunnableProgram;
	}
	
	MachineInstance getMachineInstance() {
		return this.machineInstance;
	}
	
	void setRunnableProgram(RunnableProgram runnableProgram) {
		this.runnableProgram = runnableProgram;
	}
	
	void setProgramRunResult(Integer programRunResult) {
		this.programRunResult = programRunResult;
	}
	
	Integer getProgramRunResult() {
		return this.programRunResult;
	}
	
}