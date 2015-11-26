package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;

public class StartState {
	static private boolean allProgramsRan = false;
	
	static public void setAllProgramsRan(boolean allProgramsRan) {
		StartState.allProgramsRan = allProgramsRan;
	}
	
	static public boolean getAllProgramsRan() {
		return allProgramsRan;
	}
	
	private Future<RunnableProgram> futureRunnableProgram = null;
	private MachineInstance machineInstance;
	private RunnableProgram runnableProgram = null;
	
	public StartState(Future<RunnableProgram> futureRunnableProgram, MachineInstance machineInstance) {
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
	
	RunnableProgram getRunnableProgram() {
		return this.runnableProgram;
	}

}