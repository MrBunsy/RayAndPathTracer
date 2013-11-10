package raytracer;

import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class BumpMapWater implements BumpMap{

//    private Vector[] waterDrops;
//    private double[] peaks,phases,scales;
    private double scale,time;
    private double smallNormal;
    WaterDrop[] waterDrops;
    
    public BumpMapWater(WaterDrop[] _waterDrops,double _scale){//Vector[] _waterDrops, double[] _peaks, double[] _phases, double[] _scales, double _scale){
//        waterDrops=_waterDrops;
//        peaks=_peaks;
//        scales=_scales;
//        phases=_phases;
        scale=_scale;
        
        waterDrops = _waterDrops;
        
        time=0;
        
        smallNormal=0.01;
    }
    
    public void animate(double _time){
        time=_time;
    }
    
    @Override
    public double getHeightAt(Vector pos) {
        double z = 0;

        for (int i = 0; i < waterDrops.length; i++) {
            Vector w = waterDrops[i].pos;
            double p = waterDrops[i].peak;

            double distance = pos.subtract(w).getMagnitude();

            //z += Math.cos(distance * scale) * p * (1.0 / (distance*scale*scale+1));
            z += Math.cos(time + waterDrops[i].phase + distance * scale * waterDrops[i].scale) * p;// * (1.0 / (Math.pow(distance*scale,scale)));
            
            
        }
        
        //test:
       //z += Math.cos(time + pos.x*scale)*0.2;
       // z += Math.cos(time + pos.y*scale)*0.2;
        
        return z;
    }

    @Override
    public Vector getNormalAt(Vector pos) {
         //find the surface normal
        Vector hereXwards=new Vector(pos.x+smallNormal, pos.y,getHeightAt(new Vector(pos.x+smallNormal , pos.y)));
        Vector backXwards=new Vector(pos.x-smallNormal, pos.y,getHeightAt(new Vector(pos.x-smallNormal , pos.y)));

        Vector hereYwards=new Vector(pos.x, pos.y+smallNormal,getHeightAt(new Vector(pos.x , pos.y+smallNormal)));
        Vector backYwards=new Vector(pos.x, pos.y-smallNormal,getHeightAt(new Vector(pos.x , pos.y-smallNormal)));

        //hereXwards=hereXwards.subtract(here);
        //hereYwards=hereYwards.subtract(here);

        Vector xWay=hereXwards.subtract(backXwards);
        Vector yWay=hereYwards.subtract(backYwards);

        Vector normalHere=xWay.cross(yWay).getUnit();
        
        return normalHere;
    }
    
}
