package l2f.commons.compiler;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class MemoryByteCode extends SimpleJavaFileObject
{
    private ByteArrayOutputStream oStream;
    private final String className;
    
    public MemoryByteCode(final String className, final URI uri) {
        super(uri, Kind.CLASS);
        this.className = className;
    }
    
    @Override
    public OutputStream openOutputStream() {
        return this.oStream = new ByteArrayOutputStream();
    }
    
    public byte[] getBytes() {
        return this.oStream.toByteArray();
    }
    
    @Override
    public String getName() {
        return this.className;
    }
}
