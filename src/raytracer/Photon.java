package raytracer;

import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class Photon implements OctObject,Serializable{
    public Vector pos,dir;
    public Colour colour;
    public double intensity;
    //to fit with the renderer, dir is the direction from pos to the light source
    public Photon(Vector _pos, Vector _dir, double _intensity, Colour _colour){
        pos=_pos;
        dir=_dir;
        intensity=_intensity;
        colour=_colour;
    }

    @Override
    public Vector getPos() {
        return pos;
    }
}
