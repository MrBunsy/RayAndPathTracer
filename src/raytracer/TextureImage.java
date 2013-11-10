package raytracer;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.Color;
import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public class TextureImage implements Texture{
    
     //private double xStretch,yStretch;
     private Vector stretch,offset;
     private BufferedImage image;
     private int width,height;

     public TextureImage(String textureFile){
         this(textureFile,new Vector(1,1),new Vector(0,0));
     }
     
     public TextureImage(String textureFile, Vector _stretch){
         this(textureFile,_stretch,new Vector(0,0));
     }
     
     public TextureImage(String textureFile, Vector _stretch, Vector _offset){
        stretch=_stretch;
        offset=_offset;
        try {
            image = ImageIO.read(new File(textureFile));
            width=image.getWidth();
            height=image.getHeight();
        } catch (IOException ex) {
            System.out.println("Can't read texture: "+ex.getMessage());
        }
        
    }
    
    @Override
    public Colour getColourAt(Vector pos) {
        
        pos=pos.subtract(offset);
        
        pos.x*=stretch.x;
        pos.y*=stretch.y;
        //"Other systems, such as Java, will want an integer where bits 0-7 are the blue value, 8-15 the green, and 16-23 the red."
        
        int x=(int)pos.x%width;
        int y = (int)pos.y%height;
        if(x < 0){
            x+=width;
        }
        
        if(y<0){
            y+=height;
        }
        
        int colourInt=image.getRGB(x%width, y%height);
        
        Color color = new Color(colourInt);
        
        return new Colour(color.getRed(),color.getGreen(),color.getBlue());
     //   image.createGraphics().
    }
    
}
