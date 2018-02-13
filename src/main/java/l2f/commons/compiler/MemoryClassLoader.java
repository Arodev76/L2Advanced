package l2f.commons.compiler;

import java.util.HashMap;
import java.util.Map;

public class MemoryClassLoader extends ClassLoader
{
    private final Map<String, MemoryByteCode> classes;
    private final Map<String, MemoryByteCode> loaded;
    
    public MemoryClassLoader() {
        this.classes = new HashMap<>();
        this.loaded = new HashMap<>();
    }
    
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        MemoryByteCode mbc = this.classes.get(name);
        if (mbc == null) {
            mbc = this.classes.get(name);
            if (mbc == null) {
                return super.findClass(name);
            }
        }
        return this.defineClass(name, mbc.getBytes(), 0, mbc.getBytes().length);
    }
    
    public void addClass(final MemoryByteCode mbc) {
        this.classes.put(mbc.getName(), mbc);
        this.loaded.put(mbc.getName(), mbc);
    }
    
    public MemoryByteCode getClass(final String name) {
        return this.classes.get(name);
    }
    
    public String[] getLoadedClasses() {
        return this.loaded.keySet().toArray(new String[this.loaded.size()]);
    }
    
    public void clear() {
        this.loaded.clear();
    }
}
