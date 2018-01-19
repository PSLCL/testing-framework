package com.pslcl.dtf.core.runner.rest;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Standard Json adapter for java.time.Instant fields.
 */
public class LocalDateAdaptor extends TypeAdapter<LocalDate>
{
    @Override
    public LocalDate read(JsonReader reader) throws IOException
    {
        String s = reader.nextString();
        if(s == null || s.isEmpty())
            return null;
        try
        {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e)
        {
            return null;
        }
    }

    @Override
    public void write(JsonWriter writer, LocalDate date) throws IOException
    {
        if(date == null)
            writer.nullValue();
        else
            writer.value(date.toString());
    }
}
