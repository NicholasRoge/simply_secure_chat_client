/**
 * 
 */
package roge.gui;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JTextArea;

/**
 * @author Nicholas Rogé
 */
public class ETextArea extends JTextArea{
    public static interface TextUpdateListener{
        public void recieveTextUpdate(String text);
    }
    
    private ArrayList<TextUpdateListener> __text_update_listeners=null;
    
    
    /*Begin Constructors*/
    public ETextArea(){
        super();
    }
    public ETextArea(String default_text){
        super(default_text);
    }
    /*End Constructors*/
    
    
    /*Begin Overridden Methods*/
    @Override public void setText(String text){
        super.setText(text);
        
        this._broadcastTextToListeners(text);
    }
    
    @Override public void processKeyEvent(KeyEvent event){
        super.processKeyEvent(event);
        
        if(event.getID()==KeyEvent.KEY_LAST){
            this._broadcastTextToListeners(this.getText());
        }
    }
    /*End Overridden Methods*/
    
    
    /*Begin Getter Methods*/
    protected ArrayList<TextUpdateListener> _getTextUpdateListeners(){
        if(this.__text_update_listeners==null){
            this.__text_update_listeners=new ArrayList<TextUpdateListener>();
        }
        
        return this.__text_update_listeners;
    }
    /*End Getter Methods*/
    
    
    /*Begin Other Essential Methods*/
    public void addTextUpdateListener(TextUpdateListener observer){
        this._getTextUpdateListeners().add(observer);
    }
    
    protected void _broadcastTextToListeners(String broadcast){
        for(TextUpdateListener listener:this._getTextUpdateListeners()){
            listener.recieveTextUpdate(broadcast);
        }
    }
    /*End Other Essential Methods*/
}
