package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;

/**
 * State of programs that run on machines. 
 * These are programs placed on machines from configure, run, and start template steps. 
 */
public class ProgramState {
	private MachineInstance machineInstance;
//	private Future<Integer> futureProgramRunResult = null;
//	private Integer programRunResult = null;
	private Future<RunnableProgram> futureRunnableProgram;
	private RunnableProgram runnableProgram;
	

	
//	/**
//	 * 
//	 * @param futureProgramRunResult
//	 * @param machineInstance
//	 */
//	public ProgramState(Future<Integer> futureProgramRunResult, MachineInstance machineInstance) {
//		this.futureProgramRunResult = futureProgramRunResult;
//		this.machineInstance = machineInstance;
//	}
//	
//	Future<Integer> getFutureProgramRunResult() {
//		return this.futureProgramRunResult;
//	}
//	
//	void setProgramRunResult(Integer programRunResult) {
//		this.programRunResult = programRunResult;
//	}
//	
//	Integer getProgramRunResult() {
//		return this.programRunResult;
//	}

	
	/**
	 * Constructor
	 * 
	 * @param futureRunnableProgram The Future of type RunnableProgram 
	 * @param machineInstance The MachineInstance
	 */
	public ProgramState(Future<RunnableProgram> futureRunnableProgram, MachineInstance machineInstance) {
		this.futureRunnableProgram = futureRunnableProgram;
		this.machineInstance = machineInstance;
	}

	/**
	 * 
	 * @return
	 */
	MachineInstance getMachineInstance() {
		return this.machineInstance;
	}
	
	/**
	 *
	 * @return
	 */
	Future<RunnableProgram> getFutureRunnableProgram() {
		return this.futureRunnableProgram;
	}

	/**
	 * 
	 * @param runnableProgram
	 */
	void setRunnableProgram(RunnableProgram runnableProgram) {
		this.runnableProgram = runnableProgram;
	}

	/**
	 * 
	 * @return
	 */
	RunnableProgram getRunnableProgram() {
		return this.runnableProgram;
	}
}