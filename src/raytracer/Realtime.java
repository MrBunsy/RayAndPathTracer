/*
 * Copyright Luke Wallin 2012
 */
package raytracer;

import LukesBits.Vector;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

/**
 *
 * @author Luke
 */
public class Realtime extends JFrame{
    
    private World world;
    private Dimension dims;
    private Camera camera;
    
    public Realtime(World _world){
        
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setSize(600,800);
        setMinimumSize(new Dimension(600,800));
        pack();
        
        world=_world;
        camera = world.camera;
        //camera.dir=camera.dir.add(new Vector(0,0.0001,0));
        //camera.dir=camera.dir.getUnit();
        
        //System.out.println(world.camera.dir);
        dims=getSize();
        
        camera.setupRez(dims.width, dims.height);
        
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                key(evt);
            }
        });
        
        //double buffering
        while(getBufferStrategy() == null){
            createBufferStrategy(2);
        }
        
//        addComponentListener(new ComponentAdapter() {
//        public void componentResized(ComponentEvent e){
//            beenResized(e);
//        }});
        
        setVisible(true);
    }
    
    public void key(KeyEvent evt){
        int key = evt.getKeyCode();
        switch(key){
            case KeyEvent.VK_W:
                //move forwards in camera direction
                translateCamera(0,10);
                break;
            case KeyEvent.VK_A:
                //move left in camera direction
                translateCamera(5,0);
                break;
            case KeyEvent.VK_S:
                //move backwards in camera direction
                translateCamera(0,-10);
                break;
            case KeyEvent.VK_D:
                //move right in camera direction
                translateCamera(-5,0);
                break;
            case KeyEvent.VK_DOWN:
                rotateCamera(0, 1);
                break;
            case KeyEvent.VK_UP:
                rotateCamera(0, -1);
                break;
            case KeyEvent.VK_LEFT:
                rotateCamera(-1, 0);
                break;
            case KeyEvent.VK_RIGHT:
                rotateCamera(1, 0);
                break;
            case KeyEvent.VK_ENTER:
                //render!
//                PhotonMap photonMap = new PhotonMap(world, 1, 10.0, threads);//10.0
//
//                world.setPhotonMap(photonMap);
//
//                Render render = new Render(world, threads, width, height);//, progress);
//
//                render.saveImage(outputName, quality);
                break;
        }
    }
    
    public void translateCamera(double left,double forwards){
        //move forwards:
        camera.pos = camera.pos.add(camera.dir,forwards*10);
        Vector leftDir = camera.dir.cross(new Vector(0,0,1));
        camera.pos = camera.pos.add(leftDir,left*10);
        
        camera.setupRez(dims.width, dims.height);
        repaint();
    }
    
    public void rotateCamera(double left, double up)
    {
        Vector[] screenDirs = camera.getScreenDirections();
        
        camera.dir=camera.dir.rotate(screenDirs[0], up*0.1).getUnit();
        camera.dir=camera.dir.rotate(screenDirs[1], left*0.1).getUnit();
        
        camera.setupRez(dims.width, dims.height);
        //System.out.println(world.camera.dir);
        repaint();
    }
    
    
    @Override
    public void paint(Graphics g) {
        //Graphics _g = getBufferStrategy().getDrawGraphics();
        dims=getSize();
        
        drawWorld(g);
        
//        getBufferStrategy().show();
//        _g.dispose();
    }
    
    /**
     * Draw a world onto the graphics object
     * @param world
     * @param g 
     */
    private void drawWorld(Graphics g){
        g.clearRect(0, 0, dims.width, dims.height);
        
        for(Shape s : world.scene){
            Triangle[] triangles = s.getRealtimeTriangles();
            for(Triangle t : triangles){
                Colour colour = t.getSurface().colour;
                
                Vector shapeToCamera = camera.pos.subtract(t.getCentre());
                Vector n = t.getN();
                if(n.dot(shapeToCamera) < 0){
                    //continue;
                    //triangle is facing away from us
                }
                
                if(camera.dir.dot(shapeToCamera)>0){
                    //shape is behind the camera
                    continue;
                }
                
                Vector[] vertices = t.getVertices();
                Vector a = camera.ThreeDTo2D(vertices[0]);
                Vector b = camera.ThreeDTo2D(vertices[1]);
                Vector c = camera.ThreeDTo2D(vertices[2]);
                
                int[] xPoints = new int[]{ (int)Math.round(a.x),
                                            (int)Math.round(b.x),
                                            (int)Math.round(c.x)};
                int[] yPoints = new int[]{ (int)Math.round(a.y),
                                            (int)Math.round(b.y),
                                            (int)Math.round(c.y)};
                
                g.setColor(colour.toColor());
                
                g.drawPolyline(xPoints, yPoints, 3);
            }
        }
    }
}
