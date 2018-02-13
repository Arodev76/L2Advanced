package l2f.commons.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * @author G1ta0
 */
public class ExProperties extends Properties
{
    private static final long serialVersionUID = 1L;
    public static final String defaultDelimiter = "[\\s,;]+";
    
    public void load(final String fileName) throws IOException {
        this.load(new File(fileName));
    }
    
    public void load(final File file) throws IOException {
        InputStream is = null;
        try {
            this.load(is = new FileInputStream(file));
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    public static boolean parseBoolean(final String s) {
        switch (s.length()) {
            case 1: {
                final char ch0 = s.charAt(0);
                if (ch0 == 'y' || ch0 == 'Y' || ch0 == '1') {
                    return true;
                }
                if (ch0 == 'n' || ch0 == 'N' || ch0 == '0') {
                    return false;
                }
                break;
            }
            case 2: {
                final char ch0 = s.charAt(0);
                final char ch2 = s.charAt(1);
                if ((ch0 == 'o' || ch0 == 'O') && (ch2 == 'n' || ch2 == 'N')) {
                    return true;
                }
                if ((ch0 == 'n' || ch0 == 'N') && (ch2 == 'o' || ch2 == 'O')) {
                    return false;
                }
                break;
            }
            case 3: {
                final char ch0 = s.charAt(0);
                final char ch2 = s.charAt(1);
                final char ch3 = s.charAt(2);
                if ((ch0 == 'y' || ch0 == 'Y') && (ch2 == 'e' || ch2 == 'E') && (ch3 == 's' || ch3 == 'S')) {
                    return true;
                }
                if ((ch0 == 'o' || ch0 == 'O') && (ch2 == 'f' || ch2 == 'F') && (ch3 == 'f' || ch3 == 'F')) {
                    return false;
                }
                break;
            }
            case 4: {
                final char ch0 = s.charAt(0);
                final char ch2 = s.charAt(1);
                final char ch3 = s.charAt(2);
                final char ch4 = s.charAt(3);
                if ((ch0 == 't' || ch0 == 'T') && (ch2 == 'r' || ch2 == 'R') && (ch3 == 'u' || ch3 == 'U') && (ch4 == 'e' || ch4 == 'E')) {
                    return true;
                }
                break;
            }
            case 5: {
                final char ch0 = s.charAt(0);
                final char ch2 = s.charAt(1);
                final char ch3 = s.charAt(2);
                final char ch4 = s.charAt(3);
                final char ch5 = s.charAt(4);
                if ((ch0 == 'f' || ch0 == 'F') && (ch2 == 'a' || ch2 == 'A') && (ch3 == 'l' || ch3 == 'L') && (ch4 == 's' || ch4 == 'S') && (ch5 == 'e' || ch5 == 'E')) {
                    return false;
                }
                break;
            }
        }
        throw new IllegalArgumentException("For input string: \"" + s + "\"");
    }
    
    public boolean getProperty(final String name, final boolean defaultValue) {
        boolean val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            val = parseBoolean(value);
        }
        return val;
    }
    
    public int getProperty(final String name, final int defaultValue) {
        int val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            val = Integer.parseInt(value);
        }
        return val;
    }
    
    public long getProperty(final String name, final long defaultValue) {
        long val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            val = Long.parseLong(value);
        }
        return val;
    }
    
    public double getProperty(final String name, final double defaultValue) {
        double val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            val = Double.parseDouble(value);
        }
        return val;
    }
    
    public String[] getProperty(final String name, final String[] defaultValue) {
        return this.getProperty(name, defaultValue, "[\\s,;]+");
    }
    
    public String[] getProperty(final String name, final String[] defaultValue, final String delimiter) {
        String[] val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            val = value.split(delimiter);
        }
        return val;
    }
    
    public boolean[] getProperty(final String name, final boolean[] defaultValue) {
        return this.getProperty(name, defaultValue, "[\\s,;]+");
    }
    
    public boolean[] getProperty(final String name, final boolean[] defaultValue, final String delimiter) {
        boolean[] val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            final String[] values = value.split(delimiter);
            val = new boolean[values.length];
            for (int i = 0; i < val.length; ++i) {
                val[i] = parseBoolean(values[i]);
            }
        }
        return val;
    }
    
    public int[] getProperty(final String name, final int[] defaultValue) {
        return this.getProperty(name, defaultValue, "[\\s,;]+");
    }
    
    public int[] getProperty(final String name, final int[] defaultValue, final String delimiter) {
        int[] val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            final String[] values = value.split(delimiter);
            val = new int[values.length];
            for (int i = 0; i < val.length; ++i) {
                val[i] = Integer.parseInt(values[i]);
            }
        }
        return val;
    }
    
    public long[] getProperty(final String name, final long[] defaultValue) {
        return this.getProperty(name, defaultValue, "[\\s,;]+");
    }
    
    public long[] getProperty(final String name, final long[] defaultValue, final String delimiter) {
        long[] val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            final String[] values = value.split(delimiter);
            val = new long[values.length];
            for (int i = 0; i < val.length; ++i) {
                val[i] = Long.parseLong(values[i]);
            }
        }
        return val;
    }
    
    public double[] getProperty(final String name, final double[] defaultValue) {
        return this.getProperty(name, defaultValue, "[\\s,;]+");
    }
    
    public double[] getProperty(final String name, final double[] defaultValue, final String delimiter) {
        double[] val = defaultValue;
        final String value;
        if ((value = super.getProperty(name, null)) != null && !value.isEmpty()) {
            final String[] values = value.split(delimiter);
            val = new double[values.length];
            for (int i = 0; i < val.length; ++i) {
                val[i] = Double.parseDouble(values[i]);
            }
        }
        return val;
    }
}
