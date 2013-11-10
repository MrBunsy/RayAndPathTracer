package raytracer;
import LukesBits.Vector;
import java.io.Serializable;
/**
 *
 * @author Luke
 */
public class TextureChess implements Texture{

    double cellSize;
    Colour white,black;
    
    public TextureChess(double _cellSize){
        this(_cellSize,new Colour(255,255,255),new Colour(0,100,0));
    }
    
    public TextureChess(double _cellSize, Colour _white, Colour _black){
        cellSize=_cellSize;
        white=_white;
        black=_black;
    }
    
    @Override
    public Colour getColourAt(Vector pos) {
        if(( Math.abs(Math.floor(pos.x/cellSize))%2==0 && Math.abs(Math.floor(pos.y/cellSize))%2==0  ) || (Math.abs(Math.floor(pos.x/cellSize))%2==1 && Math.abs(Math.floor(pos.y/cellSize))%2==1)){
            return white;
        }else{
            return black;
        }
    }
    
}
