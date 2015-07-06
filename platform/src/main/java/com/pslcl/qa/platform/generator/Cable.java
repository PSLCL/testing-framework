package com.pslcl.qa.platform.generator;

import java.util.UUID;

/**
 * This class represents a cable, which is a connection between a machine and a network. A machine
 * is always associated with a network even if no cable is created. Creating a cable exposes additional
 * control during the running of a test.
 */
public class Cable implements Template.Exportable {
	
	/**
	 * This class represents a reference to an IP address. This reference is evaluated when the test is run.
	 */
    public class IPReference implements Template.Parameter {
        private Machine machine;
        private Network network;

        public IPReference( Machine machine, Network network ) {
            this.machine = machine;
            this.network = network;
        }

        public String getValue(Template template) throws Exception {
            return "$(ip " + template.getReference( machine ) + " " + template.getReference( network ) + ")";
        }
    }

    private UUID exported = null;
    private UUID ipID = UUID.randomUUID();
    public Cable(Generator generator, Machine machine, Network network) {
        //TODO: Fix this
        //generator.parameters.put( ipID, new IPReference( machine, network ) );
    }

    public void connect() {
        throw new IllegalStateException( "Test not running." );
    }

    public void disconnect() {
        throw new IllegalStateException( "Test not running." );
    }

    public void toggle() {
        throw new IllegalStateException( "Test not running." );
    }

    public void setDelay(int to_machine, int from_machine) {
        throw new IllegalStateException( "Test not running." );
    }

    public void setLoss(float to_machine, float from_machine) {
        throw new IllegalStateException( "Test not running." );
    }

    public boolean isConnected() {
        throw new IllegalStateException( "Test not running." );
    }

    public String getHost() {
        // The host is not know until instantiated, so this must be a reference.
        return ipID.toString().toUpperCase();
    }

    public void export( UUID id ) {
        if ( exported != null )
            throw new IllegalStateException( "Cable is already exported." );

        exported = id;
    }

    public String getTag() {
        if ( exported == null )
            return "";

        return exported.toString().toUpperCase();
    }
}
