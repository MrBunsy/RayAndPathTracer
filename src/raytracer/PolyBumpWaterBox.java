package raytracer;

import LukesBits.Vector;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Luke
 * 
 * todo, expand this into a more general box shape class?
 */
public class PolyBumpWaterBox extends Poly {
   
    BumpMapWater water;
    
//    //quick hack to give me a box class
//    PolyBumpWaterBox(Surface _surface, Vector pos, double width, double height, double depth){
//        this(_surface, pos, width, height, depth, true);
//    }
    
    PolyBumpWaterBox(Surface _surface, Vector pos, double width, double height, double depth){//, boolean water){
        super(_surface, new Triangle[0],true);
        
        ArrayList<Triangle> _triangles = getBoxTriangles(_surface, pos, width, height, depth, true);

//        Vector[] waterDrops = new Vector[]{new Vector(width * 0.5, height * 0.5),new Vector(0, 0),new Vector(width, height),new Vector(width, 0),new Vector(0, height)};
//
//        //double[] peaks = new double[]{5.0, 3.0, 4.0, 6.0,7.0};
//        //double[] peaks = new double[]{1.4, 1.0, 0.5, 0.7,1.2};
//        //double[] peaks = new double[]{0.4, 0.6, 0.5, 0.7,0.2};
//        double[] peaks = new double[]{0.1, 0.22, 0.2, 0.19,0.1};
//        
//        
//        double[] phases = new double[]{0.3,7,2,5.7,3};
//        double[] scales = new double[]{1.0,1.3,1.2,0.9,1.05};

        //WaterDrop(Vector _pos, double _peak, double _phase, double _scale)
//        WaterDrop[] waterDrops = new WaterDrop[]{//new WaterDrop(new Vector(0, height/2), 0.2, 0, 1.0)
//                                                //,new WaterDrop(new Vector(0, height), 0.2 , 1.2, 1.7)
//                                                //,new WaterDrop(new Vector(0, 0), 0.2 , 2.2, 0.7)
//                                                //,new WaterDrop(new Vector(0, 0), 0.5 , 2.9, 2.7)
//        //                                        new WaterDrop(new Vector(width/2,height/2), 0.05, 0, 1.7)
//                                                };
        
        Random random = new Random(123);
        
        int hifreq = 0;//random.nextInt(5);
        //int midfreq = random.nextInt()
        //int lowfreq = random.nextInt(5);
        
        int drops = random.nextInt(10)+20 + hifreq;
        
        WaterDrop[] waterDrops = new WaterDrop[drops];
        
        for(int i=hifreq;i<drops;i++){
            
                waterDrops[i] = new WaterDrop(new Vector(width*random.nextDouble(),height*random.nextDouble()), 0.1*random.nextDouble(), random.nextDouble()*Math.PI*2.0, random.nextDouble()*10.0);
            //}
        }
        
        for(int i =0;i<hifreq;i++){
             //higher frequency ripples   
            waterDrops[i] = new WaterDrop(new Vector(width*random.nextDouble(),height*random.nextDouble()), 0.1*random.nextDouble(), random.nextDouble()*Math.PI*2.0, 2.0+random.nextDouble()*10.0);
        }
        
        double scale = 0.02;

        water = new BumpMapWater(waterDrops, scale);

        //set top with the bumpmap
        _triangles.get(_triangles.size() - 1).setBumpMap(water);

        for (Triangle t : _triangles) {
            t.thisIsInside(pos);
        }
        
        triangles = _triangles.toArray(new Triangle[1]);
        //Poly poly = new Poly(_surface, triangles.toArray(new Triangle[1]));
        setEncasingRadius(pos, Math.sqrt(width * width + height * height + depth * depth));
        
        for(Triangle t : triangles){
            t.setParent(this);
        }
    }
    
    //set the water droplets to a certain time for an animation
    public void animate(double time){
        water.animate(time);
    }
    
}
