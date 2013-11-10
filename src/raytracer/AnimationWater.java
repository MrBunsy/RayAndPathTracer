/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracer;

import java.io.File;
import java.util.ArrayList;
import javatree.Tree;

/**
 *
 * @author Luke
 */
public class AnimationWater implements AnimationInterface{
    private int frame;
    private double time;
    private World world;
    private long startTime;
    private int threads,width,height,quality;
    
    private String folder;
    
    private ArrayList<PolyBumpWaterBox> waterPolys;
    
    public AnimationWater(World _world, int _threads, int _width, int _height, int _quality, int skip){
        
        waterPolys=new ArrayList<PolyBumpWaterBox>();
        
        for(Shape s: _world.scene){
            if(s.getClass()==(PolyBumpWaterBox.class)){
                waterPolys.add((PolyBumpWaterBox)s);
            }
        }
        
        startTime = System.currentTimeMillis();
        frame=0;
        world=_world;
        threads=_threads;
        width=_width;
        height=_height;
        quality=_quality;
                
        folder = "animations/"+(int) (System.currentTimeMillis() / 1000L)+"_water/";
        new File(folder).mkdir();
        
        //skip a few
        for(int i=0;i<skip;i++){
            time+=Math.PI/50;
            frame++;
        }
        
        finishedImage();
    }

    @Override
    public void finishedImage() {
        if(time < Math.PI*2){
            
            time+=Math.PI/50;
            
            for(PolyBumpWaterBox p : waterPolys){
                p.animate(time);
            }
        
            Render render = new Render(world, threads, width, height,this);

            render.saveImage(folder+String.valueOf(frame)+".png",quality);
            //time+=Math.PI/100;
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
