package com.pslcl.qa.platform;

import java.util.UUID;

/**
 * Created by beastham on 5/30/2014.
 */
public class Program implements Template.Exportable {
    UUID tag = null;

    public void stop() {
        throw new IllegalStateException( "Test not running." );
    }

    public void stop( String input ) {
        throw new IllegalStateException( "Test not running." );
    }

    public int returned() {
        throw new IllegalStateException( "Test not running." );
    }

    public boolean isRunning() {
        throw new IllegalStateException( "Test not running." );
    }

    public void send(String input) {
        throw new IllegalStateException( "Test not running." );
    }

    public void receive( String output ) {
        throw new IllegalStateException( "Test not running." );
    }

    public String getTag() {
        if ( tag != null )
            return tag.toString().toUpperCase();
        return "";
    }

    public void export(UUID tag) {
        if ( this.tag != null )
            throw new IllegalStateException( "Program already exported." );

        this.tag = tag;
    }
}
