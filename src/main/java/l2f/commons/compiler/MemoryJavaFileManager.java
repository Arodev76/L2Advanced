package l2f.commons.compiler;

import java.io.IOException;
import java.net.URI;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class MemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
{
    private MemoryClassLoader cl;
    
    public MemoryJavaFileManager(final StandardJavaFileManager sjfm, final MemoryClassLoader xcl) {
        super(sjfm);
        this.cl = xcl;
    }
 
 	@SuppressWarnings("unused")
    @Override
    public JavaFileObject getJavaFileForOutput(final Location location, final String className, final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
        final MemoryByteCode mbc = new MemoryByteCode(className.replace('/', '.').replace('\\', '.'), URI.create("file:///" + className.replace('.', '/').replace('\\', '/') + kind.extension));
        this.cl.addClass(mbc);
        return mbc;
    }
    
    @Override
    public ClassLoader getClassLoader(final Location location) {
        return this.cl;
    }
}
