/**
 * 
 */
package roge.gui;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Wrapper for the JFrame class to make it a little bit more convenient to use.
 * 
 * @author Nicholas Rogé
 */
public abstract class RWindow extends JFrame{
    /**I have no idea what this is actually for, but meh.  Here's some BS documentation for it so the warning goes away.*/
    private static final long serialVersionUID = 6715060991592690674L;
    /**String to be returned in the event that this window has not had its title set yet.*/
    public static final String NO_TITLE="Untitled Window";
    
    private String __window_title=null;
    
    
    /*Begin Initializer Methods*/
    protected void _initialize(){       
        this._addMenu(this);
        this._addContent(this);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    protected void _initialize(String title){
        this.setWindowTitle(title);
        
        this._initialize();
    }
    
    protected void _addContent(final JFrame frame){
        JPanel content=new JPanel();
        
        
        this._addContent(content);
        this.setContentPane(content);
    }
    
    protected abstract void _addContent(final JPanel content);
    
    protected void _addMenu(final JFrame frame){
    }
    /*End Initializer Methods*/
    
    /*Begin Constructors*/
    /**
     * Constructs this window with the default data.
     */
    public RWindow(){
        this(RWindow.NO_TITLE);
    }
    
    /**
     * Constructs this window with the given title.
     * 
     * @param title The title this window should have.
     */
    public RWindow(String title){
        this._initialize(title);
    }
    /*End Constructors*/
    
    /*Begin Getter Methods*/
    /**
     * Gets the <code>__window_title</code> member variable.
     * 
     * @return Returns the window's title, if it has been set, or {@link RWindow#NO_TITLE} if it has not.
     */
    public String getWindowTitle(){
        if(this.__window_title==null){
            return RWindow.NO_TITLE;
        }else{
            return this.__window_title;
        }
    }
    /*End Getter Methods
    
    /*Begin Setter Method*/
    /**
     * Sets the window's title to the specified string.
     * 
     * @param title String to set the window's title to.
     * 
     * @return The <code>title</code> parameter.
     */
    public String setWindowTitle(String title){
        this.__window_title=title;
        this.setTitle(title);
        
        return title;
    }
    /*End Setter Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Changes the current content of the window.
     * 
     * @param content Content to change the window to.
     */
    public void changeContentPane(Container content){
        this.getContentPane().removeAll();
        this.setContentPane(content);
        this.validate();
        this.repaint();
    }
    /*End Other Essential Methods*/
}
