package com.pslcl.dtf.core.runner.rest;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Standard Json adapter for java.time.Instant fields.
 */
public class InstantAdaptor extends TypeAdapter<Instant>
{
    @Override
    public Instant read(JsonReader reader) throws IOException
    {
        String s = reader.nextString();
        if(s == null || s.isEmpty())
            return null;
        try
        {
            return Instant.parse(s);
        } catch (DateTimeParseException e)
        {
            return null;
        }
    }

    @Override
    public void write(JsonWriter writer, Instant timestamp) throws IOException
    {
        if(timestamp == null)
            writer.nullValue();
        else
            writer.value(timestamp.toString());
    }
}
