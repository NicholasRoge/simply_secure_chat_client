package roge.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Rogé
 */
public class ConnectionServer extends DataRecievedSubject{    
    private List<ConnectionClient> __clients;
    private BufferedReader         __input;
    private PrintWriter            __output;
    private int                    __port;
    private ServerSocket           __socket;
    private Thread                 __new_connection_listener;
    private List<Thread>           __message_listeners;
    
    /*Begin Constructors*/
    public ConnectionServer(int port){
        this.__port=port;
    }
    /*End Constructors*/
    
    /*Begin Getter Methods*/
    public List<ConnectionClient> getClientList(){
        if(this.__clients==null){
            this.__clients=new ArrayList<ConnectionClient>();
        }
        
        return this.__clients;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
    public void start(){
        try{
            this.__socket=new ServerSocket(this.__port);
            
            this._startServer();
        }catch(IOException e){
            System.out.print("IOException caught!  Message:  "+e.getMessage());
        }
    }
    
    protected void _startServer(){                
        this.__new_connection_listener=new Thread(){
            @Override public void run(){                
                try{
                    while(true){
                        ConnectionServer.this.getClientList().add(new ConnectionClient(ConnectionServer.this.__socket.accept()));
                    }
                }catch(IOException e){
                    //TODO_HIGH:  Make a handler for this exception here
                }
            }
        };
        this.__new_connection_listener.start();
    }
    /*End Other Essential Methods*/
}
