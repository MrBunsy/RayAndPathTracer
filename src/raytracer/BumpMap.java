package raytracer;

import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public interface BumpMap {
    public double getHeightAt(Vector pos);
    
    public Vector getNormalAt(Vector pos);

}
