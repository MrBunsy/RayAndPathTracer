/*
 * Copyright Luke Wallin 2012
 */
package raytracer;

/**
 *
 * @author Luke
 */
public class RenderSettings {
    
    public PhotonMap photonMap;
    public int threads;
    
    public RenderSettings(){
        photonMap = null;
        threads=0;
    }
    
    public RenderSettings setPhotonMap(PhotonMap _map){
        photonMap=_map;
        return this;
    }
}
