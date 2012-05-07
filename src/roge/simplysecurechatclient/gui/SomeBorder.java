package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Double;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

/**
 * @author Nicholas Rogé
 */
public class SomeBorder implements Border{
    private Color __inner_color;
    private Color __outer_color;
    
    /*Begin Constructors*/
    /**
     * Initializes the border with the given colors
     * 
     * @param inner_color Color for the outside walls
     * @param outer_color Color for the inside wall
     */
    public SomeBorder(Color outer_color,Color inner_color){
        this.__inner_color=inner_color;
        this.__outer_color=outer_color;
    }
    /*End Constructors*/
    
    /*Start Overridden Methods*/
    @Override public Insets getBorderInsets(Component c){
        return new Insets(3,3,3,3);
    }
    
    @Override public boolean isBorderOpaque(){
        return true;
    }
    
    @Override public void paintBorder(Component c,Graphics graphics,int x,int y,int width,int height){
        Rectangle2D inner_border[]=null;
        Insets      insets=null;
        Rectangle2D outer_borders[]=null;
        Paint       initial_paint=null;
        
        
        initial_paint=((Graphics2D)graphics).getPaint();
        
        insets=this.getBorderInsets(c);
        
        outer_borders=new Rectangle2D[4];
            outer_borders[0]=new Rectangle2D.Double(x,y,insets.left,height);//Left side
            outer_borders[1]=new Rectangle2D.Double(x+width-insets.right,y,insets.right,height); //right side
            outer_borders[2]=new Rectangle2D.Double(x,y,width,insets.top); //Top Side
            outer_borders[3]=new Rectangle2D.Double(x,y+height-insets.bottom,width,insets.bottom); //Bottom side
            
        ((Graphics2D)graphics).setPaint(__outer_color);
        for(Rectangle2D border:outer_borders){
            ((Graphics2D)graphics).fill(border);
        }
           
        inner_border=new Rectangle2D[4];
            inner_border[0]=new Rectangle2D.Double(outer_borders[0].getX()+1,outer_borders[0].getY()+1,outer_borders[0].getWidth()-2,outer_borders[0].getHeight()-2);//Left side
            inner_border[1]=new Rectangle2D.Double(outer_borders[1].getX()+1,outer_borders[1].getY()+1,outer_borders[1].getWidth()-2,outer_borders[1].getHeight()-2); //right side
            inner_border[2]=new Rectangle2D.Double(outer_borders[2].getX()+1,outer_borders[2].getY()+1,outer_borders[2].getWidth()-2,outer_borders[2].getHeight()-2); //Top Side
            inner_border[3]=new Rectangle2D.Double(outer_borders[3].getX()+1,outer_borders[3].getY()+1,outer_borders[3].getWidth()-2,outer_borders[3].getHeight()-2); //Bottom side*/
             
        ((Graphics2D)graphics).setPaint(__inner_color);
        for(Rectangle2D border:inner_border){
            if(border!=null){
                ((Graphics2D)graphics).fill(border);
            }
        }
        
        ((Graphics2D)graphics).setPaint(initial_paint);
    }
    /*End Overridden Methods*/
}
