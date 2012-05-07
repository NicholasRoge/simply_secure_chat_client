/**
 * 
 */
package roge.graphics;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * Provides a Dashed Line for use with the Graphics2D library
 * 
 * @author Nicholas Rogé
 */
public class DashedLine implements Shape{
    /**Controller which determines the frequency of the dashes in the DashedLine object*/
    public static class DashFrequency{
        /**5 dashes for every 100 pixels*/
        public final static int LOW=5;
        /**10 dashes for every 100 pixels*/
        public final static int MEDIUM=10;
        /**15 dashes for every 100 pixels*/
        public final static int HIGH=15;
    }
    
    Point __begin_point;
    int __dash_frequency;
    Point __end_point;
    
    
    /*Begin Constructors*/
    /**
     * Constructs the object with the given data.
     * 
     * @param begin_point The beginning point of the line.
     * @param end_point The end point of the line.
     * @param dash_frequency The number of dashes there should be for every 100 pixels on the longest axis.
     * 
     * @throws RuntimeException Thrown if either the <code>begin_point</code>, or <code>end_point</code> parameters are <code>null</code>;
     */
    public DashedLine(Point begin_point,Point end_point,int dash_frequency){
        if(begin_point==null||end_point==null){
            throw new RuntimeException("Neither the beginpoint or endpoint of the DashedLine object can be null.");
        }
        
        this.__begin_point=begin_point;
        this.__end_point=end_point;
        this.__dash_frequency=dash_frequency;
    }
    /*End Cosntructors*/
    
    /*Begin Overridden Methods*/
    @Override public boolean contains(Point2D point){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).contains(point);
    }

    @Override public boolean contains(Rectangle2D rectangle){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).contains(rectangle);
    }

    @Override public boolean contains(double x,double y){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).contains(x,y);
    }

    @Override public boolean contains(double x1,double y1,double x2,double y2){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).contains(x1,y1,x2,y2);
    }

    @Override public Rectangle getBounds(){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).getBounds();
    }

    @Override public Rectangle2D getBounds2D(){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).getBounds2D();
    }

    @Override public PathIterator getPathIterator(AffineTransform path_iterator){
        return this._getPath().getPathIterator(path_iterator);
    }

    @Override public PathIterator getPathIterator(AffineTransform path_iterator,double flatness){
        return this._getPath().getPathIterator(path_iterator,flatness);
    }

    @Override public boolean intersects(Rectangle2D rectangle){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).intersects(rectangle);
    }

    @Override public boolean intersects(double x1,double y1,double x2,double y2){
        return (new Line2D.Double(this.__begin_point,this.__end_point)).intersectsLine(x1,y1,x2,y2);
    }
    /*End Overridden Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Gets the the DashedLine's path.  
     * 
     * @return The the DashedLine's path.
     */
    protected GeneralPath _getPath(){
        final int line_length=Math.abs(this.__begin_point.y-this.__end_point.y);
        
        int         num_dashes=0;
        GeneralPath path=null;
        
        
        num_dashes=(line_length/100)*this.__dash_frequency;
        
        path=new GeneralPath();
        for(int counter=0,p1=0,p2=0;counter<num_dashes;counter+=2){
            p1=(int)(this.__begin_point.getY()+Math.ceil(((double)counter/(double)num_dashes)*line_length));
            p2=(int)(this.__begin_point.getY()+Math.ceil(((double)(counter+1)/(double)num_dashes)*line_length));
            
            path.append(new Line2D.Double(new Point((int)this.__begin_point.getX(),p1),new Point((int)this.__end_point.getX(),p2)),false);
        }
        
        return path;
    }
    /*End Other Essential Methods*/
}
