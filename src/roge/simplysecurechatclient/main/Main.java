package roge.simplysecurechatclient.main;

import java.awt.Dimension;

import roge.simplysecurechatclient.gui.SessionWindow;

/**
 * @author Nicholas Rogé
 */
public class Main{
    private static SessionWindow __start_session_window;
    
    
    /*Begin Main*/
    /**
     * Entry point for the application.
     * 
     * @param arguments Arguments passed to this program at runtime. 
     */
    public static void main(String[] args){
        Main.__start_session_window=new SessionWindow("Start Session");
        Main.__start_session_window.setVisible(true);
    }
    /*End Main*/
}
