package com.pslcl.dtf.sample;

import java.util.Arrays;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.core.runner.resource.ResourceNames;

public class TestConfigGenerator {
	public static void main(String[] args) throws Exception
	{
		String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
		int qa_test_id = Integer.parseInt(DTF_TEST_ID);
		Generator generator = new Generator(qa_test_id);
		Attributes linuxAttributes = new Attributes();
		Attributes windowsAttributes = new Attributes();
		
		linuxAttributes.put(ResourceNames.ImageOsKey, "linux");
		windowsAttributes.put(ResourceNames.ImageOsKey, "windows");
		
		Iterable<Artifact[]> linuxVersions = generator.createArtifactSet(null, null, "bin/testconfig.sh");
		Iterable<Artifact[]> windowsVersions = generator.createArtifactSet(null, null, "bin/testconfig.bat");
		
		for(Artifact[] nodeArtifacts : linuxVersions)
		{
			generator.startTest();
			Machine passMachine = new Machine(generator, "passLinuxMachine");
			
			passMachine.bind(new Attributes(linuxAttributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			passMachine.deploy(nodeArtifacts);
			passMachine.configure(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "pass");
			
			generator.completeTest();

			generator.startTest();
			Machine failMachine = new Machine(generator, "failLinuxMachine");
			
			failMachine.bind(new Attributes(linuxAttributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			failMachine.deploy(nodeArtifacts);
			failMachine.configure(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "fail");
			
			generator.completeTest();
		}
		
		for(Artifact[] nodeArtifacts : windowsVersions)
		{
			generator.startTest();
			Machine passMachine = new Machine(generator, "passWindowsMachine");
			
			passMachine.bind(new Attributes(windowsAttributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			passMachine.deploy(nodeArtifacts);
			passMachine.configure(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "pass");
			
			generator.completeTest();

			generator.startTest();
			Machine failMachine = new Machine(generator, "failWindowsMachine");
			
			failMachine.bind(new Attributes(windowsAttributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			failMachine.deploy(nodeArtifacts);
			failMachine.configure(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "fail");
			
			generator.completeTest();
		}

		generator.close();
	}
}
