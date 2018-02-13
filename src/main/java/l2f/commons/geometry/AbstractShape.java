package l2f.commons.geometry;

public abstract class AbstractShape implements Shape
{
    protected final Point3D max;
    protected final Point3D min;
    
    public AbstractShape() {
        this.max = new Point3D();
        this.min = new Point3D();
    }
    
    @Override
    public boolean isInside(final int x, final int y, final int z) {
        return this.min.z <= z && this.max.z >= z && this.isInside(x, y);
    }
    
    @Override
    public boolean isOnPerimeter(final int x, final int y, final int z) {
        return this.min.z <= z && this.max.z >= z && this.isOnPerimeter(x, y);
    }
    
    @Override
    public int getXmax() {
        return this.max.x;
    }
    
    @Override
    public int getXmin() {
        return this.min.x;
    }
    
    @Override
    public int getYmax() {
        return this.max.y;
    }
    
    @Override
    public int getYmin() {
        return this.min.y;
    }
    
    public AbstractShape setZmax(final int z) {
        this.max.z = z;
        return this;
    }
    
    public AbstractShape setZmin(final int z) {
        this.min.z = z;
        return this;
    }
    
    @Override
    public int getZmax() {
        return this.max.z;
    }
    
    @Override
    public int getZmin() {
        return this.min.z;
    }
}
