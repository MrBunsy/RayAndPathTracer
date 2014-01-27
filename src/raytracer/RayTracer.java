package raytracer;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import java.io.*;
import java.nio.MappedByteBuffer;
//import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;
import javatree.*;
import LukesBits.Vector;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
//import org.apache.commons.io.FileUtils;

/**
 *
 * @author Luke
 *
 * TODO - diffuse lighting doens't take into account the colour of the light
 * source
 *
 */
public class RayTracer {

    //stoled - http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
    public static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /*
             * Instead of using default, pass in a decoder.
             */
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    public static World testWorld() {
        ArrayList<Light> lights = new ArrayList<Light>();

        lights.add(new Light(new Vector(500, -50, 1000), 500000));
        lights.add(new Light(new Vector(500, 500, 800), 500000));



        Surface redSphere = new Surface(new Colour(255, 0, 0));
        redSphere.setGloss(3);

        Surface sphereSurface2 = new Surface(new Colour(255, 0, 200));
        sphereSurface2.setGloss(15).setReflective(0.1);

        Surface sphereSurface3 = new Surface(new Colour(255, 0, 200));
        sphereSurface3.setGloss(40).setReflective(1).setAmbient(0);

        Surface planeSurface = new Surface(new Colour(0, 200, 0));
        planeSurface.setReflective(0.1).setTexture(new TextureChess(50));
        Surface planeSurface2 = new Surface(new Colour(255, 255, 255));
        planeSurface2.setReflective(1);

        ArrayList<Shape> scene = new ArrayList<Shape>();

        scene.add(new Sphere(redSphere, new Vector(0, 0, 0), 30));

        scene.add(new Sphere(sphereSurface2, new Vector(0, 70, 10), 30));
        scene.add(new Sphere(sphereSurface3, new Vector(-100, -80, 40), 40));

        scene.add(new Plane(planeSurface, new Vector(0, 0, -30), new Vector(0, 0, 1)));


        scene.add(new Plane(planeSurface2, new Vector(-500, 0, 0), new Vector(1, 0, -0.5)));

        Camera camera = new Camera(new Vector(200, 000, 10), new Vector(-1, 0, -0.1), 20, 32);

        World world = new World(scene, lights, camera, 5);

        return world;
    }

    public static World mirrorBalls() {

        ArrayList<Light> lights = new ArrayList<Light>();
        ArrayList<Shape> scene = new ArrayList<Shape>();

        lights.add(new Light(new Vector(500, -50, 1000), 500000));
        lights.add(new Light(new Vector(500, 500, 800), 500000));

        Surface planeSurface = new Surface(new Colour(0, 200, 0));
        planeSurface.setReflective(0.1).setTexture(new TextureChess(50)).setAmbient(0.2);

        Surface reflectiveSurface = new Surface(new Colour(0, 0, 0));
        reflectiveSurface.setReflective(1).setGloss(40);

        Surface glass = new Surface(new Colour(255, 0, 200));
        glass.setGloss(40).setReflective(0).setAmbient(0).setClear(1).setDiffuse(0).setn(1.4);

        double apart = 100;
        double r = 40;
        double startx = 0 - 500;
        double starty = 140 - 700;
        double z = 0;

        int i = 0;

        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 20; y++) {

                i++;
                double tempx = startx + x * apart;
                double tempy = starty + y * apart;

                scene.add(new Sphere(x % 2 == 0 || y % 2 == 1 ? reflectiveSurface : glass, new Vector(tempx, tempy, z), r));
            }
        }
        scene.add(new Plane(planeSurface, new Vector(0, 0, -60), new Vector(0, 0, 1)));

        Camera camera = new Camera(new Vector(750, 400, 350), new Vector(-1, 0, -0.9), 20, 32);
        World world = new World(scene, lights, camera, 5);

        return world;
    }

    public static void printUsage() {
        System.out.println("Usage: "
                + "-w --width Image width (pixels)\n"
                + "-h --height Image height (pixels)\n"
                + "-t --threads Number of threads\n"
                + "-s --scene Location of JSON world file\n"
                + "-i --image PNG image name\n"
                + "-a --animate Produce an animation\n"
                + "-q --quality AA factor\n"
                + "-p --noPhoton Don't use a photon map\n"
                + "--savePhoton Name of file to save high quality photon map to\n"
                + "--loadPhoton Name of file to load photon map from\n"
                + "--skip How many frames to skip into the animation\n"
                + "--animation Which animation to run: \"treeGrow\", \"sunlight\", \"water\"");
    }

    public static void writeFile(String file, String text) {
        writeFile(file, text, false);
    }

    public static void writeFile(String file, String text, boolean append) {
        try {
            FileWriter fstream = new FileWriter(file, append);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.valueOf(text));

            out.close();

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        CmdLineParser parser = new CmdLineParser();


        Option widthArg = parser.addIntegerOption('w', "width");
        Option heightArg = parser.addIntegerOption('h', "height");
        Option threadsArg = parser.addIntegerOption('t', "threads");
        Option qualityArg = parser.addIntegerOption('q', "quality");
        Option worldArg = parser.addStringOption('s', "scene");
        Option outputArg = parser.addStringOption('i', "image");
        Option animationArg = parser.addBooleanOption('a', "animate");
        Option realtimeArg = parser.addBooleanOption('r', "realtime");
        // Option progressArg = parser.addBooleanOption('p', "photon");
        Option savePhotonMapArg = parser.addStringOption("savePhoton");
        Option loadPhotonMapArg = parser.addStringOption("loadPhoton");
        Option noPhotonMapArg = parser.addBooleanOption('p', "noPhoton");
        Option skipArg = parser.addIntegerOption("skip");
        Option animationTypeArg = parser.addStringOption("animation");
        Option pathTraceArg = parser.addBooleanOption("pathtrace");
        Option samplesArg = parser.addIntegerOption("samples");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
        }

        int width = (Integer) parser.getOptionValue(widthArg, 1024);
        int height = (Integer) parser.getOptionValue(heightArg, 768);
        int threads = (Integer) parser.getOptionValue(threadsArg, Runtime.getRuntime().availableProcessors());
        String worldFile = (String) parser.getOptionValue(worldArg, "world.json");
        String outputName = (String) parser.getOptionValue(outputArg, "images/" + (int) (System.currentTimeMillis() / 1000L));// + ".png"
        boolean animate = (Boolean) parser.getOptionValue(animationArg, false);
        boolean realtime = (Boolean) parser.getOptionValue(realtimeArg, false);
        boolean noPhotonMap = (Boolean) parser.getOptionValue(noPhotonMapArg, false);
        // boolean progress = (Boolean) parser.getOptionValue(progressArg, false);
        //how much anti-aliasing
        int quality = (Integer) parser.getOptionValue(qualityArg, 1);
        String animationType = (String) parser.getOptionValue(animationTypeArg, "sunlight");
        int skip = (Integer) parser.getOptionValue(skipArg, 0);

        String savePhotonMapName = (String) parser.getOptionValue(savePhotonMapArg, "");
        boolean savePhotonMap = !savePhotonMapName.isEmpty();

        String loadPhotonMapName = (String) parser.getOptionValue(loadPhotonMapArg, "");
        boolean loadPhotonMap = !loadPhotonMapName.isEmpty();
        boolean pathTrace = (Boolean)parser.getOptionValue(pathTraceArg,false);
        int samples = (Integer)parser.getOptionValue(samplesArg, 256);
        //if we're scaling down the image later, scale up the rendered image
        width *= quality;
        height *= quality;

        World world;
        try {
            System.out.print("Loading world...");
            System.out.println(new File(".").getAbsolutePath());
            world = World.importJSON(readFile(worldFile));
            System.out.print("\rLoaded world: " + world.name + "\n");
        } catch (IOException ex) {
            Logger.getLogger(RayTracer.class.getName()).log(Level.SEVERE, null, ex);
            world = testWorld();
        }


        //world.scene.add(Poly.getTumbler(new Vector(0,0,0), new Vector(0.1,0,1), 100, 200,true));
        //world.scene.add(Poly.getRubbishSphere(Surface.glass().setn(1.2), new Vector(0,0,100), 100,20,20));
        //world.scene.add(new Sphere(Surface.glass().setn(1.0), new Vector(0,0,100), 100));//.setn(1.0)
        //world.scene.addAll(Poly.getTumbler(new Vector(0,0,0), new Vector(0.1,0,1), 100, 200,true));

        //world.scene.add(new PolyCylinder(Surface.glass(), new Vector(0,0,0), new Vector(0,0,1), 40, 100, 20, 1));
        //world.scene.add(new PolyCylinder(Surface.glass().setn(1.2), new Vector(0,0,0), new Vector(0.1,0,1), 200, 100, 40, 1));

        if (realtime) {
            //produce a real time version of the world
            Realtime r = new Realtime(world);
        } else if(pathTrace){
            PathTraceRender render = new PathTraceRender(world, threads, width, height,samples);//, progress);

            render.saveImage(outputName, quality);
        }
        else{

            if (animate) {
                if (animationType.equals("sunlight")) {
                    AnimationSunlight animation = new AnimationSunlight(world, threads, width, height, quality, skip);
                } else if (animationType.equals("treeGrow")) {

                    TreeParameters params = new TreeParameters();
                    params.seed = 123456;
                    params.tipRadius = 1;

                    Tree tree = new Tree(params);
                    AnimationTreeGrow animation = new AnimationTreeGrow(world, threads, width, height, quality, tree, skip);
                } else if (animationType.equals("water")) {
                    AnimationWater waterAnimation = new AnimationWater(world, threads, width, height, quality, skip);
                }
            } else {


                if (!noPhotonMap) {


                    if (savePhotonMap) {

                        //this is a photon map to be saved - therefore higher quality!
                        PhotonMap photonMap = new PhotonMap(world, 10, 2.0, threads);
                        //PhotonMap photonMap = new PhotonMap(world, 1, 10.0,threads);

                        System.out.println("Saving photon map to " + savePhotonMapName);

                        FileOutputStream fos = null;
                        ObjectOutputStream out = null;
                        try {
                            fos = new FileOutputStream(savePhotonMapName);
                            out = new ObjectOutputStream(fos);
                            out.writeObject(photonMap);
                            out.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        System.exit(0);
                    }
                    PhotonMap photonMap = null;

                    if (loadPhotonMap) {

                        System.out.println("Loading photon map from " + loadPhotonMapName);

                        FileInputStream fis = null;
                        ObjectInputStream in = null;
                        try {
                            fis = new FileInputStream(loadPhotonMapName);
                            in = new ObjectInputStream(fis);
                            photonMap = (PhotonMap) in.readObject();
                            in.close();
                            //world.setPhotonMap(photonMap);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }

                    } else {
                        //neither loading nor saving, so just generate one
                        photonMap = new PhotonMap(world, 1, 10.0, threads);//10.0
                    }
                    world.setPhotonMap(photonMap);

                } else {
                    System.out.println("Skipping photon map");
                }
                Render render = new Render(world, threads, width, height);//, progress);

                render.saveImage(outputName, quality);
            }
        }
    }

    public static String tidyTime(long miSeconds) {
        String time = "";

        long seconds = miSeconds / 1000;

        if (seconds > 60 * 60) {
            //hours
            time += (int) Math.floor((double) seconds / (60.0 * 60.0)) + "h, ";
        }

        if (seconds > 60) {
            //minutes
            time += (int) Math.floor((double) seconds / (60.0)) + "m, ";
        }

        time += (seconds % 60) + "." + (miSeconds % 1000) + "s";

        return time;
    }

    //yoinked from http://nakkaya.com/2009/11/08/command-line-progress-bar/
    //and then tweaked
    public static void printProgBar(double dpercent) {
        StringBuilder bar = new StringBuilder("[");

        dpercent *= 100;
        int percent = (int) Math.round(dpercent);
        dpercent = Math.round(dpercent * 100.0) / 100.0;
        //dpercent=Math.round(dpercent);

        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                bar.append("=");
            } else if (i == (percent / 2)) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }

        bar.append("]   " + dpercent + "%     ");
        System.out.print("\r" + bar.toString());
    }
}
