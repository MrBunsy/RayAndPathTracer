package raytracer;
import java.awt.Color;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class Colour implements Serializable{
//    public int r;
//    public int g;
//    public int b;
    
    private double r,g,b;
    
    //used for this colour representing the colour of a light
    //private double intensity;

    public Color toColor(){
        //return new Color((int)Math.round(tidy(r)),(int)Math.round(tidy(g)),(int)Math.round(tidy(b)));
        return new Color((float)r/255.0f,(float)g/255.0f,(float)b/255.0f);
    }
    
    public Colour(double _r,double _g,double _b){
        r=_r;
        g=_g;
        b=_b;
        //intensity=0;
    }
    
//    public double getIntensity(){
//        return intensity;
//    }
//    
//    public Colour setIntensity(double _intensity){
//        if(Double.isInfinite(_intensity)){
//            System.out.println("INFINITE");
//
//        }
//        intensity=_intensity;
//        return this;
//    }
    
    public double getR(){
        return r;
    }
    
    public double getG(){
        return g;
    }
    
    public double getB(){
        return b;
    }
    
    public int getIntR(){
        return (int)Math.round(r);
    }
    
    public int getIntG(){
        return (int)Math.round(g);
    }
    
    public int getIntB(){
        return (int)Math.round(b);
    }
    //dim:0-1
    public Colour dim(double dim){
        //int test = tidy( (int)Math.round((double)r*dim) );
        
        return new Colour(tidy(r*dim ),tidy( g*dim ),tidy( b*dim));
    }

    //bit of a hack to deal with colour light on an object
    public Colour times(Colour colour){
        double timesr = (double)colour.getR()/255.0;
        double timesg = (double)colour.getG()/255.0;
        double timesb = (double)colour.getB()/255.0;
        
        return new Colour(tidy( (int)Math.round((double)r*timesr) ),tidy( (int)Math.round((double)g*timesg) ),tidy( (int)Math.round((double)b*timesb) ));
    }
    
    public Colour add(Colour colour){
        return new Colour(tidy(r+colour.r),tidy(g+colour.g),tidy(b+colour.b));
    }

    private double tidy(double me){
        if(me<0)
            me=0;

        if(me>255)
            me=255;
        return me;
    }
    //return
    //0xAARRGGBB
//    public function returnGD(img){
//        /*
//        redHex=dechex(this.r);
//        if(strlen(redHex)<2)
//            redHex="0".redHex;
//
//        greenHex=dechex(this.g);
//        if(strlen(greenHex)<2)
//            greenHex="0".greenHex;
//
//        blueHex=dechex(this.b);
//        if(strlen(blueHex)<2)
//            blueHex="0".blueHex;
//
//        return "0x00".redHex.greenHex.blueHex;*/
//
//
//        return imagecolorallocate(img,this.r,this.g,this.b);
//
//    }
    //methods like get brighter, make duller

    @Override
    public Colour clone(){
        return new Colour(r, g, b);
    }

    public String toString(){
        return "("+r+","+g+","+b+")";
    }
}
