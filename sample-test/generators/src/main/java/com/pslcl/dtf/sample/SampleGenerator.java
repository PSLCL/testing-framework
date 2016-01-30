package com.pslcl.dtf.sample;

import java.util.Arrays;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

public class SampleGenerator {
	public static void main(String[] args) throws Exception
	{
		String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
		int qa_test_id = Integer.parseInt(DTF_TEST_ID);
		Generator generator = new Generator(qa_test_id);
		Attributes attributes = new Attributes();
		attributes.put(ProviderNames.InstanceTypeKey, "t2.micro");
		
		Iterable<Artifact[]> versions = generator.createArtifactSet(null, null, "bin/test.sh");
		
		for(Artifact[] nodeArtifacts : versions)
		{
			generator.startTest();
			Machine passMachine = new Machine(generator, "passMachine");
			
			passMachine.bind(new Attributes(attributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			passMachine.deploy(nodeArtifacts);
			passMachine.run(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "pass");
			
			generator.completeTest();

			generator.startTest();
			Machine failMachine = new Machine(generator, "failMachine");
			
			failMachine.bind(new Attributes(attributes).putAll( nodeArtifacts[0].getModule().getAttributes() ));
			failMachine.deploy(nodeArtifacts);
			failMachine.run(Arrays.asList(nodeArtifacts), nodeArtifacts[0].getName(), "fail");
			
			generator.completeTest();
		}

		generator.close();
	}
}
