package com.dell.gumshoe.util;

import static com.dell.gumshoe.util.Output.configure;
import static com.dell.gumshoe.util.Output.debug;
import static com.dell.gumshoe.util.Output.error;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {
    private final String[] prefixes;
    private final Properties combinedProperties;

    public Configuration() throws IOException {
        prefixes = new String[] { "gumshoe." };
        combinedProperties = new Properties(System.getProperties());
        initProperties();
    }

    public Configuration(Properties p) {
        prefixes = new String[] { "gumshoe." };
        combinedProperties = new Properties(System.getProperties());
        combinedProperties.putAll(p);

    }
    public Configuration(Configuration delegate, String prefix) {
        this.prefixes = initPrefixes(prefix, delegate.prefixes);
        combinedProperties = delegate.combinedProperties;
    }

    public Configuration withPrefix(String prefix) {
        return new Configuration(this, prefix);
    }

    private static String[] initPrefixes(String newPrefix, String[] delegatePrefixes) {
        if(delegatePrefixes.length==1) {
            final String[] out = { "gumshoe."+newPrefix+".", "gumshoe." };
            return out;
        } else if(delegatePrefixes.length==2) {
            final String[] out = { delegatePrefixes[0] + newPrefix + ".", delegatePrefixes[1]+newPrefix+"." };
            return out;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Properties initProperties() throws IOException {
        final String name = System.getProperty("gumshoe.config", "gumshoe.properties");

        // don't show verbose output yet
        // properties determine if and where to print the messages
        // so collect them for now and handle in finally block
        final List<String> deferredMessages = new ArrayList<>();
        try {
            try {
                final Properties resourceProperties = readPropertyClasspath(name, deferredMessages);
                if(resourceProperties!=null) { combinedProperties.putAll(resourceProperties); }

                final Properties fileProperties = readPropertyFile(name, deferredMessages);
                if(fileProperties!=null) { combinedProperties.putAll(fileProperties); }
                return combinedProperties;
            } finally {
                // configure output system with as many properties as we could find
                configure(this);
                for(String msg : deferredMessages) {
                    debug(msg);
                }
            }
        } catch(IOException|RuntimeException e) {
            error(e, "failed to read properties");
            throw e;
        }
    }

    private Properties readPropertyFile(String name, List<String> output) throws IOException {
        final File file = new File(name);
        if( ! file.isFile() || ! file.canRead()) { return null; }

        output.add("GUMSHOE: reading configuration file");
        final Properties out = new Properties();
        final Reader in = new FileReader(file);
        try {
            out.load(in);
            return out;
        } finally {
            in.close();
        }
    }

    private Properties readPropertyClasspath(String name, List<String> output) throws IOException {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        if(in==null) { return null; }

        output.add("GUMSHOE: reading configuration resource");
        final Properties out = new Properties();
        try {
            out.load(in);
            return out;
        } finally {
            in.close();
        }
    }

    /////

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defaultValue) {
        for(String prefix : prefixes) {
            final String value = combinedProperties.getProperty(prefix + key);
            if(value!=null) { return value; }
        }
        return defaultValue;
    }

    public boolean isTrue(String key, boolean defaultValue) {
        return "true".equalsIgnoreCase(getProperty(key, Boolean.toString(defaultValue)));
    }

    public long getNumber(String key, long defaultValue) {
        final Long value = getNumber(key);
        return value==null ? defaultValue : value;
    }

    public Long getNumber(String key) {
        final String stringValue = getProperty(key);
        return stringValue==null ? null : Long.parseLong(stringValue);
    }

    public String[] getList(String key) {
        final String stringValue = getProperty(key);
        if(stringValue==null || stringValue.isEmpty()) { return new String[0]; }
        final String[] out = stringValue.split(",");
        for(int i=0;i<out.length;i++) {
            out[i] = out[i].trim();
        }
        return out;
    }

}
