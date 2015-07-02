package com.pslcl.qa.platform.generator;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents a network, which in combination with cables allows for
 * machines to communicate with each other.
 */
public class Network extends Resource {
    private static final Hash hash = Hash.fromContent("network");

    /**
     * Create a new network, associated with a generator and a name.
     * @param generator The generator that can use the network.
     * @param name The name of the network, used for logging and debugging.
     */
    public Network(Generator generator, String name) {
        super(generator, name, hash );
    }

    public String getDescription() {
        return "Network description.";
    }
}
