package com.pslcl.dtf.sample;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Person;

public class TestInspectGenerator {
	public static void main(String[] args) throws Exception
	{
		String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
		int qa_test_id = Integer.parseInt(DTF_TEST_ID);
		Generator generator = new Generator(qa_test_id);

		Iterable<Artifact[]> passVersions = generator.createArtifactSet(null, null, "pass_instructions.html");
		Iterable<Artifact[]> failVersions = generator.createArtifactSet(null, null, "fail_instructions.html");
		
		for(Artifact[] passInstructions : passVersions)
		{
			generator.startTest();
			Person passPerson = new Person(generator, "passPerson");
			
			passPerson.bind();
			passPerson.inspect(passInstructions[0].getContent(), passInstructions); //Include instructions artifacts as attachments for testing.
			
			generator.completeTest();
		}
		
		for(Artifact[] failInstructions : failVersions)
		{
			generator.startTest();
			Person passPerson = new Person(generator, "passPerson");
			
			passPerson.bind();
			passPerson.inspect(failInstructions[0].getContent(), failInstructions);
			
			generator.completeTest();
		}

		generator.close();
	}
}
