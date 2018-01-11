package l2f.commons.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

import java.io.IOException;
import java.net.URI;

public class MemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
{
	private MemoryClassLoader cl;

	public MemoryJavaFileManager(StandardJavaFileManager sjfm, MemoryClassLoader xcl)
	{
		super(sjfm);
		cl = xcl;
	}

	@SuppressWarnings("unused")
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
	{
		MemoryByteCode mbc = new MemoryByteCode(className.replace('/', '.').replace('\\', '.'), URI.create("file:///" + className.replace('.', '/').replace('\\', '/') + kind.extension));
		cl.addClass(mbc);

		return mbc;
	}

	@Override
	public ClassLoader getClassLoader(Location location)
	{
		return cl;
	}
}
