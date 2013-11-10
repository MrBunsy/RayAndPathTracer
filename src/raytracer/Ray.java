package raytracer;

import LukesBits.Vector;
/**
 *
 * @author Luke
 */
public class Ray {
    //3d vector
    public Vector start;
    //3D unit vector
    public Vector dir;
    public Shape onSurfaceOf;

    public Ray(Vector _start,Vector _dir){
        start=_start;
        dir=_dir.getUnit();
    }
}
