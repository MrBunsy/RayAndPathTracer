package raytracer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import LukesBits.Vector;
import javatree.Tree;
import javatree.TreeParameters;

/**
 *
 * @author Luke
 */
public class World {
    public ArrayList<Shape> scene;
    public ArrayList<Light> lights;
    public Camera camera;
    public int iterate;
    private PhotonMap photonMap;
    public String name;
    
    private Sunlight sunlight;
    
    //public _photonMap;
    //public _screen;

    public World(ArrayList<Shape> _scene,ArrayList<Light> _lights,Camera _camera,int _iterate){
        this(_scene, _lights, _camera, _iterate,null);
    }
    
    public World(ArrayList<Shape> _scene,ArrayList<Light> _lights,Camera _camera,int _iterate,Sunlight _sunlight){//,_photonMap=true){//iterate deafult=5
        scene=_scene;
        lights=_lights;
        camera=_camera;
        //_photonMap=_photonMap;
        iterate=_iterate;
        sunlight=_sunlight;
        //_screen=_screen;
    }
    
    public boolean hasPhotonMap(){
        return photonMap!=null;
    }
    
    public PhotonMap getPhotonMap(){
        return photonMap;
    }
    
    public void setPhotonMap(PhotonMap _photonMap){
        photonMap=_photonMap;
    }
    
    /**
     * Is this world sunlit?
     * @return True is there is sunlight
     */
    public boolean isSunlight(){
        return sunlight!=null;
    }
    
    public Sunlight getSunlight(){
        return sunlight;
    }
    
    public void setSunlight(Sunlight _sunlight){
        sunlight=_sunlight;
    }
    
    public double getAirN(){
        return 1.0;
    }
    
    private static Vector vectorFromJson(JSONObject jpos) throws JSONException{
        
        double z = jpos.has("z") ? jpos.getDouble("z") : 0;
        
        Vector vector = new Vector(jpos.getDouble("x"),jpos.getDouble("y"),z);
        
        return vector;
    }
    
    private static Colour colourFromJson(JSONObject jcolour) throws JSONException{
        Colour colour = new Colour(jcolour.getInt("r"),jcolour.getInt("g"),jcolour.getInt("b"));
        
        return colour;
    }
    
    private static Surface surfaceFromJson(JSONObject jsurface) throws JSONException{
        Surface surface = new Surface(colourFromJson(jsurface.getJSONObject("colour")));
        
        if(jsurface.has("ambient")){
            surface.setAmbient(jsurface.getDouble("ambient"));
        }
        if(jsurface.has("clear")){
            surface.setClear(jsurface.getDouble("clear"));
        }
        
        if(jsurface.has("diffuse")){
            surface.setDiffuse(jsurface.getDouble("diffuse"));
        }
        
        if(jsurface.has("gloss")){
            surface.setGloss(jsurface.getDouble("gloss"));
        }
        
        if(jsurface.has("reflective")){
            surface.setReflective(jsurface.getDouble("reflective"));
        }
        
        if(jsurface.has("skybox")){
            surface.setSkybox(jsurface.getBoolean("skybox"));
        }
        
        if(jsurface.has("texture")){
            
            JSONObject jtexture = jsurface.getJSONObject("texture");
            
            if(jtexture.getString("type").equals("image")){
                
                Vector stretch = jtexture.has("stretch") ? vectorFromJson(jtexture.getJSONObject("stretch")) : new Vector (1,1);
                Vector offset = jtexture.has("offset") ? vectorFromJson(jtexture.getJSONObject("offset")) : new Vector(0,0);
                
                surface.setTexture(new TextureImage("textures/"+jtexture.getString("image"),stretch,offset));
                        
            }else if (jtexture.getString("type").equals("chess")){
                //this is probably a bit archaic, remove?
                surface.setTexture(new TextureChess(50));
            }
        }
        
        if(jsurface.has("n")){
            surface.setn(jsurface.getDouble("n"));
        }
        
        return surface;
    }

    public static JSONObject exportVector(Vector v) throws JSONException{
        JSONObject j = new JSONObject();

        j.put("x", v.x);
        j.put("y", v.y);
        j.put("z", v.z);
        
        return j;
    }
    
    public static JSONObject exportPoly(Poly poly){
        return exportPoly(poly, new Vector(0,0,0));
    }
    public static JSONObject exportPoly(Poly poly, Vector pos){
        JSONObject j = new JSONObject();
        
        try {
            j.put("type", "poly");
            j.put("polytype","general");
            j.put("pos",exportVector(pos));
            
            JSONArray jtriangles = new JSONArray();
            
            Triangle[] triangles = poly.getTriangles();
            
            for(Triangle t : triangles){
                JSONObject jtriangle = new JSONObject();
                jtriangle.put("v0", exportVector(t.v0));
                jtriangle.put("v1", exportVector(t.v1));
                jtriangle.put("v2", exportVector(t.v2));
                
                if(t.isPhong()){
                    Vector[] ns = t.getNs();
                    jtriangle.put("n0", exportVector(ns[0]));
                    jtriangle.put("n1", exportVector(ns[1]));
                    jtriangle.put("n2", exportVector(ns[2]));
                }
                
                jtriangles.put(jtriangle);
            }
            
            j.put("triangles", jtriangles);
            
        } catch (JSONException ex) {
            Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return j;
    }
    
    public static World importJSON(String jsonString) {
        ArrayList<Light> lights = new ArrayList<Light>();
        ArrayList<Shape> scene = new ArrayList<Shape>();
        String name="";

        Camera camera = new Camera(new Vector(750, 400, 350), new Vector(-1, 0, -0.9), 20, 32);
        
        Sunlight sunlight = null;
        
        int iterations = 5;
        
        JSONObject j;
        try {
            j = new JSONObject(jsonString);
            
            if (j.getInt("version") ==2){
                
                if(j.has("name")){
                    //System.out.println("World Name: "+j.getString("name"));
                    name=j.getString("name");
                }
                
                if(j.has("iterations")){
                    iterations = j.getInt("iterations");
                }
                
                if(j.has("lights")){
                    JSONArray jsonLights = j.getJSONArray("lights");
                    
                    for(int l=0;l<jsonLights.length();l++){
                        JSONObject jlight = jsonLights.getJSONObject(l);
                        
                        Vector pos = vectorFromJson(jlight.getJSONObject("pos"));
                        
                        Colour colour = jlight.has("colour") ? colourFromJson(jlight.getJSONObject("colour")) : new Colour(255,255,255);
                        
                        Light light = new Light(pos,jlight.getDouble("intensity"), colour);
                        lights.add(light);
                    }
                }
                
                if(j.has("sunlight")){
                    JSONObject jsonSunlight = j.getJSONObject("sunlight");
                    Vector angle = vectorFromJson(jsonSunlight.getJSONObject("angle"));
                    Colour colour = colourFromJson(jsonSunlight.getJSONObject("colour"));
                    double brightness = jsonSunlight.getDouble("brightness");
                    
                    sunlight=new Sunlight(angle, colour, brightness);
                }
                
                JSONArray jsonShapes = j.getJSONArray("shapes");
                
                for(int s=0;s<jsonShapes.length();s++){
                    JSONObject jshape = jsonShapes.getJSONObject(s);
                    
                    //not using switch because want this to run on java1.6 too
                    if(jshape.getString("type").equals("sphere")){// ------------- sphere ------------
                        
                        
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        double r = jshape.getDouble("r");
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        Sphere sphere = new Sphere(surface, pos, r);
                        
                        scene.add(sphere);
                        
                    }else if(jshape.getString("type").equals("plane")){// ------------- plane ------------
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        Vector angle = vectorFromJson(jshape.getJSONObject("angle"));
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        Plane plane = new Plane(surface, pos, angle);
                        
                        scene.add(plane);
                    }else if(jshape.getString("type").equals("triangle")){// ------------- triangle ------------
                        Vector v0 = vectorFromJson(jshape.getJSONObject("v0"));
                        Vector v1 = vectorFromJson(jshape.getJSONObject("v1"));
                        Vector v2 = vectorFromJson(jshape.getJSONObject("v2"));
                        
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        Triangle triangle = new Triangle(surface, v0, v1, v2);
                        
                        scene.add(triangle);
                    }else if(jshape.getString("type").equals("poly") && jshape.getString("polytype").equals("sphere")){// ------------- poly sphere ------------
                        //todo expand to all polys
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        double r = jshape.getDouble("r");
                        
                        Poly poly = Poly.getRubbishSphere(surface, pos, r,20,20);
                        
                        scene.add(poly);
                    }else if(jshape.getString("type").equals("poly") && jshape.getString("polytype").equals("cylinder")){// ------------- poly cylinder ------------
                        //todo expand to all polys
                        Vector base = vectorFromJson(jshape.getJSONObject("base"));
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        double r = jshape.getDouble("r");
                        double h = jshape.getDouble("h");
                        Vector dir = vectorFromJson(jshape.getJSONObject("dir"));
                        int quality = jshape.has("quality") ? jshape.getInt("quality") : 20;
                        
                        PolyCylinder poly = new PolyCylinder(surface, base, dir, h, r, quality, 1);
                        
                        scene.add(poly);
                    }else if(jshape.getString("type").equals("waterBlock")){// ------------- water block ------------
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        double width = jshape.getDouble("width");
                        double height = jshape.getDouble("height");
                        double depth = jshape.getDouble("depth");
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        PolyBumpWaterBox water = new PolyBumpWaterBox(surface, pos, width, height, depth);
                        
                        scene.add(water);
                    }else if(jshape.getString("type").equals("skybox")){// ------------- skybox ------------
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        double width = jshape.getDouble("width");
                        double height = jshape.getDouble("height");
                        double depth = jshape.getDouble("depth");
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        ArrayList<Plane> skybox = Plane.getSkyBox(surface, pos, width, height, depth);
                        
                        scene.addAll(skybox);
                    }else if(jshape.getString("type").equals("box")){// ------------- box ------------
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        double width = jshape.getDouble("width");
                        double height = jshape.getDouble("height");
                        double depth = jshape.getDouble("depth");
                        Surface surface = surfaceFromJson(jshape.getJSONObject("surface"));
                        
                        Poly box = Poly.getBox(surface, pos, width, height, depth);
                        
                        scene.add(box);
                        
                    }else if(jshape.getString("type").equals("tumbler")){// ------------- tumbler ------------
                        
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        double r = jshape.getDouble("radius");
                        double height = jshape.getDouble("height");
                        //upright with water by default
                        Vector dir = jshape.has("angle") ? vectorFromJson(jshape.getJSONObject("angle")) : new Vector(0,0,1);
                        boolean water = jshape.has("water") ? jshape.getBoolean("water") : true;
                        
                        scene.addAll(Poly.getTumbler(pos, dir, r, height,water));
                        
                    }else if(jshape.getString("type").equals("tree")){// ------------- tree ------------
                        Vector pos = vectorFromJson(jshape.getJSONObject("pos"));
                        Vector angle = vectorFromJson(jshape.getJSONObject("angle"));
                        double growTo = jshape.getDouble("growTo");
                        
                        TreeParameters params = new TreeParameters();
                        
                        params.pos = pos;
                        params.angle=angle;
                        
                        if(jshape.has("scale") && !jshape.has("height")){
                            params.scale=jshape.getDouble("scale");
                        }
                        
                        
                        
                        if(jshape.has("gravity")){
                            params.gravity = vectorFromJson(jshape.getJSONObject("gravity"));
                        }
                        if(jshape.has("light")){
                            params.light = vectorFromJson(jshape.getJSONObject("light"));
                        }
                        if(jshape.has("wind")){
                            params.wind = vectorFromJson(jshape.getJSONObject("wind"));
                        }
                        if(jshape.has("seed")){
                            params.seed = jshape.getLong("seed");
                        }
                        if(jshape.has("density")){
                            params.density = jshape.getDouble("density");
                        }
                        if(jshape.has("tipRadius")){
                            params.tipRadius = jshape.getDouble("tipRadius");
                        }
                        if(jshape.has("elasticity")){
                            params.elasticity = jshape.getDouble("elasticity");
                        }
                        if(jshape.has("branchChange")){
                            params.branchChance = jshape.getDouble("branchChance");
                        }
                        if(jshape.has("muBranchLength")){
                            params.muBranchLength = jshape.getDouble("muBranchLength");
                        }
                        if(jshape.has("sigmaBranchLength")){
                            params.sigmaBranchLength= jshape.getDouble("sigmaBranchLength");
                        }
                        if(jshape.has("minBranchLength")){
                            params.minBranchLength = jshape.getDouble("minBranchLength");
                        }
                        if(jshape.has("growthRate")){
                            params.growthRate = jshape.getDouble("growthRate");
                        }
                        if(jshape.has("growthRateChange")){
                            params.growthRateChange = jshape.getDouble("growthRateChange");
                        }
                        if(jshape.has("muNumChildren")){
                            params.muNumChildren= jshape.getDouble("muNumChildren");
                        }
                        if(jshape.has("sigmaNumChildren")){
                            params.sigmaNumChildren = jshape.getDouble("sigmaNumChildren");
                        }
                        
                        Tree tree = new Tree(params);
        
                        for (double x = 0; x < growTo; x += 0.1) {
                                tree.grow(0.1);
                            }
                        
                        if(jshape.has("height")){
                            double wantHeight=jshape.getDouble("height");
                            
                            double scale = wantHeight/tree.getMaxHeight();
                            tree.setScale(scale);
                        }
                        
                        int quality = jshape.has("quality") ? jshape.getInt("quality") : 20;
                        
                        //ArrayList<Shape> treePolys = Poly.tree(tree,quality);
                        //scene.addAll(treePolys);
                        PolyTree polytree = new PolyTree(tree, quality);
                        scene.add(polytree);
                    }
                }
                
                
                JSONObject jcamera = j.getJSONObject("camera");
                
                Vector pos = vectorFromJson(jcamera.getJSONObject("pos"));
                Vector dir = vectorFromJson(jcamera.getJSONObject("dir"));
                
                double lensDistance = jcamera.getDouble("lensDistance");
                double lensSize = jcamera.getDouble("lensSize");
                
                camera = new Camera(pos, dir, lensDistance, lensSize);
            }else{
                System.out.println("Sorry, world specification version not supported");
                System.exit(0);
            }
        } catch (JSONException ex) {
            //Logger.getLogger(RayTracer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Failed to read JSON, check specification");
            System.exit(0);
        }
        

        World world = new World(scene, lights, camera, iterations);
        
        world.name=name;
        world.setSunlight(sunlight);
        
        return world;
    }
}
