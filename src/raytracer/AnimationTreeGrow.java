package raytracer;

import java.io.File;
import java.util.ArrayList;
import javatree.Tree;

/**
 *
 * @author Luke
 */
public class AnimationTreeGrow implements AnimationInterface {

    private int frame;
    private World world;
    private ArrayList<Shape> originalScene;
    private String folder;
    private int threads, width, height,quality;
    //0 = sunrise, 1= sunset
    private double time;
    private long startTime;
    private Tree tree;

    public AnimationTreeGrow(World _world, int _threads, int _width, int _height, int _quality, Tree _tree, int skip) {
        startTime = System.currentTimeMillis();
        frame = 0;
        world = _world;
        threads = _threads;
        width = _width;
        height = _height;
        originalScene=(ArrayList<Shape>)world.scene.clone();
        tree=_tree;
        quality=_quality;
        frame=0;
        
        folder = "animations/"+(int) (System.currentTimeMillis() / 1000L)+"_tree/";
        new File(folder).mkdir();
        
        //skip a few
        for(int i=0;i<skip;i++){
            time+=0.1;
            tree.grow(0.1);
            frame++;
        }
        
        finishedImage();
    }

    @Override
    public void finishedImage() {
        
        if(time < 70){
            
            time+=0.1;
            tree.grow(0.1);
            //clear the old tree growth
            world.scene=(ArrayList<Shape>)originalScene.clone();

    //        for (double x = 0; x < growTo; x += 0.1) {
    //                tree.grow(0.1);
    //            }

            ArrayList<Shape> treePolys = Poly.tree(tree);
            world.scene.addAll(treePolys);
        
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
