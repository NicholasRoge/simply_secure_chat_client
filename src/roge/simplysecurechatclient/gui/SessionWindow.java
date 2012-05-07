/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import roge.gui.RWindow;
import roge.simplysecurechatclient.main.Main;

/**
 * Window allowing users to enter their session key and begin a chat session.
 * 
 * @author Nicholas Rogé
 */
public class SessionWindow extends RWindow{
    
    /*Begin Constructors*/
    /**
     * Initializes the window to its default settings.
     */
    public SessionWindow(){
        this("");
    }
    
    /**
     * Initializes the window with its title as the given string.
     * 
     * @param title Title of the window.
     */
    public SessionWindow(String title){
        super(title);
    }
    /*End Constructors*/
    
    /*Overridden Methods*/
    @Override protected void _addContent(JPanel content){
        this._displayGetSessionTypePanel(content);
    }
    /*End Overridden Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Creates the panel which shows
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _displayGetSessionTypePanel(JPanel content){
        JButton            host_session=null;
        JButton            join_session=null;
        
        content.setLayout(new GridLayout(2,1));
            host_session=new JButton("Host Session");
            host_session.setBorder(new SomeBorder(Color.BLACK,Color.WHITE));
            host_session.setBackground(Color.BLACK);
            host_session.setForeground(Color.WHITE);
            content.add(host_session);
        
            join_session=new JButton("Join Session");
            join_session.setBorder(new SomeBorder(Color.BLACK,Color.WHITE));
            join_session.setBackground(Color.BLACK);
            join_session.setForeground(Color.WHITE);
            host_session.addActionListener(new ActionListener(){
                @Override public void actionPerformed(ActionEvent arg0){
                    JPanel content=null;
                    
                    
                    content=new JPanel();
                    SessionWindow.this._displayHostKey(content);
                    
                    SessionWindow.this.changeContentPane(content);
                }
                
            });
            content.add(join_session);
            
            
        this.setSize(new Dimension(300,200));
        this.setResizable(false);
    }
    
    protected void _displayHostKey(JPanel content){
        JButton test=null;
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
            test=new JButton("Trololol");
            content.add(test);
    }
    
    protected void _displayJoinHost(JPanel content){
        
    }
    /*End Other Essential Methods*/
}

