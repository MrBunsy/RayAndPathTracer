package raytracer;
import LukesBits.Vector;
/**
 *
 * @author Luke
 * 
 * used to pass answers to refraction stuff around.
 */
public class Refraction{
    public Vector refract,reflect;
    public double T,R;
    
    //did total internal reflection occur?
    public boolean tir(){
        return R==1.0;
    }
    
    
    public Refraction(Vector _refract, Vector _reflect, double _T, double _R){
        refract=_refract;
        reflect=_reflect;
        T=_T;
        R=_R;
    }
}
