package raytracer;
import LukesBits.Vector;
import java.io.Serializable;
/**
 *
 * @author Luke
 */
public class Light{// extends Shape {
    public double intensity;
    public Vector unitPos;
    
    public Colour colour;
    public Vector pos;

    //idea - have a make 1 thing for colour, reduces rgb to between 0 and 1, then this can be multiplied by the surface colour to simulate tinted light!
    public Light(Vector _pos,double _intensity){
        this(_pos,_intensity,new Colour(255,255,255));
    }
    
    public Light(Vector _pos,double _intensity,Colour _colour) {
        pos=_pos;

        if(_colour==null){
            _colour=new Colour(255,255,255);
        }

        colour=_colour;

        intensity=_intensity;
        unitPos=_pos.getUnit();
    }

    public double lightAt(Vector _pos){
        double distance=_pos.subtract(pos).getMagnitude();

        if(distance==0){
            distance=1;
        }
        //1/r^2
        return intensity/Math.pow(distance,2);
    }

    public double lightAtDistance(double _distance){
         return intensity/Math.pow(_distance,2);
    }
    //uses distance^2, which might likely already exist
    public double lightAtDistanceSqrd(double _distance){
         return intensity/_distance;
    }
}
