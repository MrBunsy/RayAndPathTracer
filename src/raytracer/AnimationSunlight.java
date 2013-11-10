package raytracer;

import LukesBits.Vector;
import java.io.File;

/**
 *
 * @author Luke
 * 
 * idea is this simulates the sun rising and setting
 * 
 */
public class AnimationSunlight implements AnimationInterface {

    private int frame;
    private World world;
    private String folder;
    private int threads,width,height,quality;
    
    //0 = sunrise, 1= sunset
    private double time;
    
    private long startTime;
    //private double sunHighestAngle;
    
    private double moveSpeed;
    
    private Vector yDir,xDir;
    
    //modelling the sun moving as if it were moving around the edge of a circle
    //this circle is at an angle with the ground - taken as the x-y plane
    //the circle can also be offset above or below the ground, so far this is going to take it as march when it is not offset
    public AnimationSunlight(World _world, int _threads, int _width, int _height, int _quality, int skip){
        startTime = System.currentTimeMillis();
        frame=0;
        world=_world;
        threads=_threads;
        width=_width;
        height=_height;
        quality=_quality;
                
        folder = "animations/"+(int) (System.currentTimeMillis() / 1000L)+"_sunlight/";
        
        //when the sun is at its highest, this is the angle between the plane and the height of teh sun
        double sunHighestAngle = Math.PI*0.7;
        //this is the anlge inline with the plane and when the sun is highest - essentially where south is.
        double sunHighestPlaneAngle = Math.PI/2;
        
        xDir = new Vector(Math.cos(sunHighestPlaneAngle),Math.sin(sunHighestPlaneAngle),0);
        yDir = xDir.cross(new Vector(0,0,1)).add(new Vector(0,0,Math.sin(sunHighestAngle)));//new Vector(Math.cos(sunHighestPlaneAngle),Math.sin(sunHighestPlaneAngle),Math.sin(sunHighestAngle));
        time=0;
        new File(folder).mkdir();
        
        moveSpeed=Math.PI/200;
        
        //skip frames
        for(int i=0;i<skip;i++){
            time+=moveSpeed;
            frame++;
        }
        
        finishedImage();
    }
    
    @Override
    public void finishedImage() {
        
        
        if(time < Math.PI){
            //move sun
            Vector sunPos = xDir.multiply(Math.cos(time)).add(yDir.multiply(Math.sin(time)));
            double brightness = 0.6;
            
            
            //during evening and morning there is less blue in the sunlight as this is scattered by the atmosphere
            world.setSunlight(new Sunlight(sunPos,new Colour(255,(int)Math.round(Math.pow(Math.sin(time*0.9 + Math.PI*0.05),0.25)*220.0),(int)Math.round(Math.pow(Math.sin(time),0.5)*255.0)),brightness));
            Render render = new Render(world, threads, width, height,this);

            render.saveImage(folder+String.valueOf(frame)+".png",quality);
            time+=moveSpeed;
        }else{
            //finished!
            long end = System.currentTimeMillis();
            long time = end - startTime;

            System.out.println("Animation Time taken: " + ((double) time / 1000.0) + "s = " + ( time / 60000) + "m, "+(time%60000)/ 1000.0+"s");
            System.out.println("Frames: "+frame);
            System.out.println("Average time per frame: "+ Math.round((((double) time / (1000.0))/(double)frame)*100.0)/100.0 + "s");
        }
        frame++;
    }
    
}
