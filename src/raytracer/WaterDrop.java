package raytracer;

import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public class WaterDrop {
    public Vector pos;
    public double peak,phase,scale;
    
    public WaterDrop(Vector _pos, double _peak, double _phase, double _scale){
        pos=_pos;
        peak=_peak;
        phase=_phase;
        scale=_scale;
    }
}
