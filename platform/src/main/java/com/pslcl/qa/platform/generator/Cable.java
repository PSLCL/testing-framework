package com.pslcl.qa.platform.generator;

import java.util.UUID;

/**
 * This class represents a cable, which is a connection between a machine and a network. A machine
 * is always associated with a network even if no cable is created. Creating a cable exposes additional
 * control during the running of a test.
 */
public class Cable {
	private String ipReference = UUID.randomUUID().toString().toUpperCase(); 
	
	private UUID exported;
	
	/**
	 * This class represents a reference to an IP address. This reference is evaluated when the test is run.
	 */
    private class IPReference implements Template.Parameter {
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

    public Cable(Generator generator, Machine machine, Network network) {
        generator.addParameterReference(ipReference, new IPReference(machine, network));
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
    
    /**
     * Get a reference to an IP address which may be used as a parameter in a program action.
     * 
     * This reference will be resolved by the Generator when the Template is generated. If the value of
     * the IP address is known at that time, then the reference will be replaced by the value. If the value
     * of the IP address will not be known until the test is run, then the reference will be replaced by
     * a value reference in the form of $(ip <machine-ref> <network-ref>).
     * .
     * @return a String reference to the IP address.
     */
    public String getIPReference(){
    	return ipReference;
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
