package raytracer;
import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class Surface {
    
    //0=not at all, 1= fully
    //diffuse surfaces reflect all the light shining on them off in equal amounts at all angles.
    public double diffuse;

    //is this shiny? 2+, proper name, specular
    public double gloss;
    //clear or coloured?
    public double clear;

    //0=not at all, 1=mirror!
    public double reflective;
    //refractive index
    public double n;
    
    //used in special cases where the outside of this surface is not air
    private double outsideN;
    private boolean hasOutsideN;
    
    
    public Colour colour;

    //public boolean texture;
    
    public Texture texture;
    
    //do we ignore this shape for sunlight?
    private boolean skybox;
    
//    public BufferedImage textureImage;
//    
//    public double xStretch,yStretch;
    
    //is this lit by ambient lighting?
    public double ambient;

    public Colour getColour(Vector pos){
        if(texture==null){
            return colour;
        }else{
            return texture.getColourAt(pos);
        }
        
        
//        if(( Math.abs(Math.floor(relPos.x/50))%2==0 && Math.abs(Math.floor(relPos.y/50))%2==0  ) || (Math.abs(Math.floor(relPos.x/50))%2==1 && Math.abs(Math.floor(relPos.y/50))%2==1)){
//            return new Colour(255,255,255);
//        }else{
//            return new Colour(0,100,0);
//        }
    }

   

    public Surface (Colour _colour){//,diffuse=1,reflective=0,gloss=0,clear=0,n=1){
        /*diffuse=diffuse;
        clear=clear;
        reflective=reflective;
        n=n;
        colour=colour;
        gloss=gloss;*/
        diffuse=1;
        clear=0;
        reflective=0;
        n=1.5;
        colour=_colour;
        gloss=0;
        ambient=0.1;
        texture=null;
        skybox=false;
        hasOutsideN=false;
    }

    @Override
    public Surface clone(){
        Surface s = new Surface(colour.clone());
        
        s.setAmbient(ambient);
        s.setClear(clear);
        s.setDiffuse(diffuse);
        s.setGloss(gloss);
        s.setOutsideN(outsideN);
        s.setReflective(reflective);
        s.setSkybox(skybox);
        //TODO clone method for texture?
        s.setTexture(texture);
        s.setn(n);
    
        return s;
    }
    
    public static Surface water(){
        Surface water = new Surface(new Colour(255,255,255));
        water.setClear(1);
        water.setAmbient(0);
        water.setn(1.333);
        
        return water;
    }
    
    public static Surface diamond(){
        Surface glass = new Surface(new Colour(255,255,255));
        glass.setClear(1);
        glass.setAmbient(0);
        glass.setn(2.419);
        
        return glass;
    }
    
    public static Surface glass(){
        Surface glass = new Surface(new Colour(255,255,255));
        glass.setClear(1);
        glass.setAmbient(0);
        glass.setn(1.5);
        
        return glass;
    }
    
//    public Texture getTexture(){
//        return texture;
//    }
//    
//    public boolean hasTexture(){
//        return texture!=null;
//    }
    
    public boolean isSkybox(){
        return skybox;
    }
    
    public Surface setSkybox(boolean _skybox){
        skybox=_skybox;
        return this;
    }
    
    public Surface setDiffuse(double _diffuse){
        diffuse=_diffuse;
        return this;
    }

    public Surface setClear(double _clear){
        clear=_clear;
        return this;
    }

    public Surface setReflective(double _reflective){
        reflective=_reflective;
        return this;
    }

    public Surface setn(double _n){
        n=_n;
        return this;
    }

    public Surface setOutsideN(double _outsideN){
        outsideN=_outsideN;
        hasOutsideN=true;
        return this;
    }
    
    public boolean hasOutsideN(){
        return hasOutsideN;
    }
    
    public double getOutsideN(){
        return outsideN;
    }
    
    public Surface setColour(Colour _colour){
        colour=_colour;
        return this;
    }

    public Surface setGloss(double _gloss){
        gloss=_gloss;
        return this;
    }

//    public Surface setTexture(String textureFile,double _xStretch, double _yStretch){
//        texture=true;
//        xStretch=_xStretch;
//        yStretch=_yStretch;
//        try {
//            textureImage = ImageIO.read(new File(textureFile));
//        } catch (IOException ex) {
//            System.out.println("Can't read texture: "+ex.getMessage());
//            texture=false;
//        }
//        
//        return this;
//    }
    
    public Surface setTexture(Texture _texture){
        texture=_texture;
        return this;
    }

    public Surface setAmbient(double _ambient){
        ambient=_ambient;
        return this;
    }
    
    public boolean isDiffuse(){
        return diffuse > 0;
    }
    
    public boolean isGloss(){
        return gloss > 0;
    }
    
    public double getDiffuse(){
        return diffuse;
    }
    
    public double getGloss(){
        return gloss;
    }
}
