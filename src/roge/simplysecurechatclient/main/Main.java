package roge.simplysecurechatclient.main;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import roge.net.ConnectionClient;
import roge.net.ConnectionServer;
import roge.net.DataRecievedSubject.DataReceivedListener;
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
        boolean is_server=true;
        
        /*
        Main.__start_session_window=new SessionWindow("Start Session");
        Main.__start_session_window.setVisible(true);
         */
        
        if(is_server){
            ConnectionServer server=new ConnectionServer(1337);
            server.start();
        }else{
            ConnectionClient client=new ConnectionClient("192.168.2.10",1337);
            client.connect();
            client.addDataRecievedListener(new DataReceivedListener(){
    
                @Override
                public void receiveData(String data){
                    System.out.print(data+"\n");
                }
                
            });
            
            try{
                client.send("Lulz!  Your security sucks!");
                System.out.print("Message sent.");
            }catch(IOException e){
                System.out.print("Your message could not be sent.  Cause:  "+e.getMessage());
            }
            
            
            client.disconnect();
        }
    }
    /*End Main*/
}
