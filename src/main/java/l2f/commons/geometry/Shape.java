package l2f.commons.geometry;

public interface Shape
{
	public boolean isInside(int x, int y);

	public boolean isInside(int x, int y, int z);

    boolean isOnPerimeter(final int p0, final int p1);
    
    boolean isOnPerimeter(final int p0, final int p1, final int p2);
	
	public Point2D[] getBoundaries();
	
	public int getXmax();

	public int getXmin();

	public int getYmax();

	public int getYmin();

	public int getZmax();

	public int getZmin();
}
