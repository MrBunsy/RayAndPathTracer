package raytracer;

import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 * 
 * Not entirely sure if this deserves its own class, but here goes
 */
public class Sunlight{
    
    //angle is the angle from the world to the sun
    private Vector sunAngle;
    private Colour sunColour;
    private double sunBrightness;
    
    public Sunlight(Vector angle, Colour colour,double brightness){
        sunAngle=angle.getUnit();
        sunColour=colour;
        sunBrightness=brightness;
    }
    
    public Vector getAngle(){
        return sunAngle;
    }
    
    public Colour getColour(){
        return sunColour;
    }
    
    public double getBrightness(){
        return sunBrightness;
    }
    
}
