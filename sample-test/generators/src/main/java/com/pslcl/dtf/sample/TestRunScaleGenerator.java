package com.pslcl.dtf.sample;

import java.util.Arrays;
import java.util.List;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;
import com.pslcl.dtf.core.runner.resource.ResourceNames;

public class TestRunScaleGenerator {
	public static void main(String[] args) throws Exception
	{
		int testCount = 0;
		try{ 
			testCount = Integer.parseInt(args[0]);
		} catch(Exception e){
			System.out.println("Expected integer as first argument specifying test count.");
			return;
		}
		String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
		int qa_test_id = Integer.parseInt(DTF_TEST_ID);
		Generator generator = new Generator(qa_test_id);
		Attributes linuxAttributes = new Attributes();
		
		linuxAttributes.put(ResourceNames.ImageOsKey, "linux");
		
		Iterable<Artifact[]> linuxVersions = generator.createArtifactSet(null, null, "bin/testrunscale.sh");
		
		for(Artifact[] nodeArtifacts : linuxVersions)
		{
			for(int i = 0; i < testCount; i++){
				generator.startTest();
				Machine machine = new Machine(generator, "LinuxMachineScale-" + i);
				
				machine.bind(new Attributes(linuxAttributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
				List<Action> passDeployActions = machine.deploy(nodeArtifacts);
				machine.run(passDeployActions, nodeArtifacts[0].getName(), Integer.toString(i));
				
				generator.completeTest();
			}
		}

		generator.close();
	}
}
