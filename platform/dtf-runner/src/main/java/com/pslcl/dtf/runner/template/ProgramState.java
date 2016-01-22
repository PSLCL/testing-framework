package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;

/**
 * State of programs that run on machines. 
 * These are programs placed on machines from configure, run, and start template steps. 
 */
public class ProgramState {
	// TODO: with work, this double use of this class can be consolidated, by making this an abstract class that has common setters and getters 
	private MachineInstance machineInstance;
	private Future<Integer> futureProgramRunResult = null;
	private Integer programRunResult = null;
	private Future<RunnableProgram> futureRunnableProgram = null;
	private RunnableProgram runnableProgram = null;
	
	MachineInstance getMachineInstance() {
		return this.machineInstance;
	}
	
	
	// the section for configure
	
	/**
	 * 
	 * @param futureProgramRunResult
	 * @param machineInstance
	 */
	public ProgramState(Future<Integer> futureProgramRunResult, MachineInstance machineInstance) {
		this.futureProgramRunResult = futureProgramRunResult;
		this.machineInstance = machineInstance;
	}
	
	Future<Integer> getFutureProgramRunResult() {
		return this.futureProgramRunResult;
	}
	
	void setProgramRunResult(Integer programRunResult) {
		this.programRunResult = programRunResult;
	}
	
	Integer getProgramRunResult() {
		return this.programRunResult;
	}

	
	// the section for run and start
	
	/**
	 * 
	 * @param futureRunnableProgram
	 * @param machineInstance
	 * @param dummy Ignored, used only to differentiate two constructors
	 */
	public ProgramState(Future<RunnableProgram> futureRunnableProgram, MachineInstance machineInstance, int dummy) {
		this.futureRunnableProgram = futureRunnableProgram;
		this.machineInstance = machineInstance;
	}
	
	Future<RunnableProgram> getFutureRunnableProgram() {
		return this.futureRunnableProgram;
	}

	void setRunnableProgram(RunnableProgram runnableProgram) {
		this.runnableProgram = runnableProgram;
	}

	RunnableProgram getRunnableProgram() {
		return this.runnableProgram;
	}
}