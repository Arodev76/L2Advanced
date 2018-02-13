package l2f.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import l2f.commons.geometry.Point2D;
import l2f.commons.geometry.Point3D;
import l2f.commons.geometry.Shape;
import l2f.commons.util.Rnd;
import l2f.gameserver.geodata.GeoEngine;
import l2f.gameserver.templates.spawn.SpawnRange;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.PositionUtils;

public class Territory implements Shape, SpawnRange
{
    protected final Point3D max;
    protected final Point3D min;
    private final List<Shape> include;
    private final List<Shape> exclude;
    
    public Territory() {
        this.max = new Point3D();
        this.min = new Point3D();
        this.include = new ArrayList<Shape>(1);
        this.exclude = new ArrayList<Shape>(1);
    }
    
    public Territory add(final Shape shape) {
        if (this.include.isEmpty()) {
            this.max.x = shape.getXmax();
            this.max.y = shape.getYmax();
            this.max.z = shape.getZmax();
            this.min.x = shape.getXmin();
            this.min.y = shape.getYmin();
            this.min.z = shape.getZmin();
        }
        else {
            this.max.x = Math.max(this.max.x, shape.getXmax());
            this.max.y = Math.max(this.max.y, shape.getYmax());
            this.max.z = Math.max(this.max.z, shape.getZmax());
            this.min.x = Math.min(this.min.x, shape.getXmin());
            this.min.y = Math.min(this.min.y, shape.getYmin());
            this.min.z = Math.min(this.min.z, shape.getZmin());
        }
        this.include.add(shape);
        return this;
    }
    
    public Territory addBanned(final Shape shape) {
        this.exclude.add(shape);
        return this;
    }
    
    public List<Shape> getTerritories() {
        return this.include;
    }
    
    public List<Shape> getBannedTerritories() {
        return this.exclude;
    }
    
    public boolean isInside(final int x, final int y) {
        for (int i = 0; i < this.include.size(); ++i) {
            final Shape shape = this.include.get(i);
            if (shape.isInside(x, y)) {
                return !this.isExcluded(x, y);
            }
        }
        return false;
    }
    
    public boolean isInside(final int x, final int y, final int z) {
        if (x < this.min.x || x > this.max.x || y < this.min.y || y > this.max.y || z < this.min.z || z > this.max.z) {
            return false;
        }
        for (int i = 0; i < this.include.size(); ++i) {
            final Shape shape = this.include.get(i);
            if (shape.isInside(x, y, z)) {
                return !this.isExcluded(x, y, z);
            }
        }
        return false;
    }
    
    public boolean isOnPerimeter(final int x, final int y) {
        return this.isInside(x, y);
    }
    
    public boolean isOnPerimeter(final int x, final int y, final int z) {
        return this.isInside(x, y, z);
    }
    
    public boolean isInside(final GameObject obj) {
        return this.isInside(obj.getLoc());
    }
    
    public boolean isInside(final Location loc) {
        return this.isInside(loc.x, loc.y, loc.z);
    }
    
    public boolean isExcluded(final int x, final int y) {
        for (int i = 0; i < this.exclude.size(); ++i) {
            final Shape shape = this.exclude.get(i);
            if (shape.isInside(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isExcluded(final int x, final int y, final int z) {
        for (int i = 0; i < this.exclude.size(); ++i) {
            final Shape shape = this.exclude.get(i);
            if (shape.isInside(x, y, z)) {
                return true;
            }
        }
        return false;
    }
    
    public int getXmax() {
        return this.max.x;
    }
    
    public int getXmin() {
        return this.min.x;
    }
    
    public int getYmax() {
        return this.max.y;
    }
    
    public int getYmin() {
        return this.min.y;
    }
    
    public int getZmax() {
        return this.max.z;
    }
    
    public int getZmin() {
        return this.min.z;
    }
    
    public static Location getRandomLoc(final Territory territory) {
        return getRandomLoc(territory, 0);
    }
    
    public static Location getRandomLoc(final Territory territory, final int geoIndex) {
        final Location pos = new Location();
        final List<Shape> territories = territory.getTerritories();
    Label_0307:
        for (int i = 0; i < 100; ++i) {
            final Shape shape = territories.get(Rnd.get(territories.size()));
            pos.x = Rnd.get(shape.getXmin(), shape.getXmax());
            pos.y = Rnd.get(shape.getYmin(), shape.getYmax());
            pos.z = shape.getZmin() + (shape.getZmax() - shape.getZmin()) / 2;
            if (territory.isInside(pos.x, pos.y)) {
                final int tempz = GeoEngine.getHeight(pos, geoIndex);
                if (shape.getZmin() != shape.getZmax()) {
                    if (tempz < shape.getZmin()) {
                        continue;
                    }
                    if (tempz > shape.getZmax()) {
                        continue;
                    }
                }
                else {
                    if (tempz < shape.getZmin() - 200) {
                        continue;
                    }
                    if (tempz > shape.getZmin() + 200) {
                        continue;
                    }
                }
                pos.z = tempz;
                final int geoX = pos.x - World.MAP_MIN_X >> 4;
                final int geoY = pos.y - World.MAP_MIN_Y >> 4;
                for (int x = geoX - 1; x <= geoX + 1; ++x) {
                    for (int y = geoY - 1; y <= geoY + 1; ++y) {
                        if (GeoEngine.NgetNSWE(x, y, tempz, geoIndex) != 15) {
                            continue Label_0307;
                        }
                    }
                }
                return pos;
            }
        }
        return pos;
    }
    
    public double getDistance(final Location loc) {
        return PositionUtils.getDistance((this.getXmin() + this.getXmax()) / 2, (this.getYmin() + this.getYmax()) / 2, loc.x, loc.y);
    }
    
    public Location getRandomLoc(final int geoIndex) {
        return getRandomLoc(this, geoIndex);
	}

	@Override
	public Point2D[] getBoundaries()
	{
		if (!include.isEmpty())
			return include.get(0).getBoundaries(); // Its lame, but it will be like that for now.
		
		return null;
	}
	
	@Override
	public String toString()
	{
		return "xMin=" + getXmin() + " xMax=" + getXmax() + " yMin=" + getYmin() + " yMax=" + getYmax() + " zMin=" + getZmin() + " zMax=" + getZmax();
	}
}