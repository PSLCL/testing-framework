package com.pslcl.dtf.sample;

import java.util.Arrays;
import java.util.List;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.core.generator.resource.Network;
import com.pslcl.dtf.core.generator.template.Program;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;
import com.pslcl.dtf.core.runner.resource.ResourceNames;

public class TestNetworkGenerator {
    public static void main(String[] args) throws Exception
    {
        String DTF_TEST_ID = System.getenv("DTF_TEST_ID");
        int qa_test_id = Integer.parseInt(DTF_TEST_ID);
        Generator generator = new Generator(qa_test_id);
        Attributes linuxAttributes = new Attributes();

        linuxAttributes.put(ResourceNames.ImageOsKey, "linux");

        Iterable<Artifact[]> clientVersions = generator.createArtifactSet(null, null, "bin/testnetworkclient.sh");
        Iterable<Artifact[]> serverVersions = generator.createArtifactSet(null, null, "bin/testnetworkserver.sh");

        for(Artifact[] serverArtifacts : serverVersions)
        {
            for(Artifact[] clientArtifacts : clientVersions)
            {

                generator.startTest();

                Machine serverMachine = new Machine(generator, "serverMachine");
                Machine clientMachine = new Machine(generator, "clientMachine");
                Network network = new Network(generator, "network");

                serverMachine.bind(new Attributes(linuxAttributes).putAll( serverArtifacts[0].getModule().getAttributes() ));
                clientMachine.bind(new Attributes(linuxAttributes).putAll( serverArtifacts[0].getModule().getAttributes() ));
                network.bind();

                String serverIP = serverMachine.connect(network).getIPReference();
                clientMachine.connect(network);

                List<Action> serverDependencyActions = serverMachine.deploy(serverArtifacts);
                List<Action> clientDependencyActions = clientMachine.deploy(clientArtifacts);

                Program serverProgram = serverMachine.start(serverDependencyActions, serverArtifacts[0].getName());

                clientDependencyActions.add(serverProgram.getProgramAction());
                clientMachine.run(clientDependencyActions, clientArtifacts[0].getName(), serverIP);

                generator.completeTest();
            }
        }

        generator.close();
    }
}
