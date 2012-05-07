package roge.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Nicholas Rogé
 */
public class ConnectionClient extends DataRecievedSubject{    
    private String         __host_address;
    private BufferedReader __input;
    private Thread         __message_listener;
    private PrintWriter    __output;
    private int            __port;
    private Socket         __socket;
    
    /*Begin Constructors*/
    /**
     * 
     * 
     * @param socket
     */
    public ConnectionClient(Socket socket){
        this.__socket=socket;
        
        this.__host_address=this.__socket.getInetAddress().getHostAddress();
        this.__port=this.__socket.getPort();
        this._connect();
    }
    
    public ConnectionClient(String host_address,int port){
        this.__host_address=host_address;
        this.__port=port;
    }
    /*End Constructors*/
    
    /*Begin Other Essential Methods*/
    public void connect(){
        if(this.__socket==null){
            try{
                this.__socket=new Socket(this.__host_address,this.__port);
            }catch(UnknownHostException e){
                System.out.print("Could not connect to the specified host:  "+this.__host_address+"\n");
            }catch(IOException e){
                System.out.print("IOException caught!  Message:  "+e.getMessage());
            }
        }
        
        this._connect();
    }
    
    protected void _connect(){
        try{
            this.__input=new BufferedReader(new InputStreamReader(this.__socket.getInputStream()));
            this.__output=new PrintWriter(this.__socket.getOutputStream(),true);
            
            this._startMessageListener();
        }catch(IOException e){
            System.out.print("IOException caught!  Message:  "+e.getMessage());
        }
    }
    
    public void send(String data) throws IOException{
        if(this.__socket==null){
            throw new IOException("You must initialize the connection before you can send anything.");
        }
        
        this.__output.write(data);
    }
    
    protected void _startMessageListener(){
        this.__message_listener=new Thread(){
            @Override public void run(){
                String data=null;
                
                
                while(true){
                    try{
                        data=ConnectionClient.this.__input.readLine();
                        if(data==null){
                            break;
                        }else{
                            ConnectionClient.this.broadcastData(data);
                        }
                    }catch(IOException e){
                        System.out.print("IOException caught!  Message:  "+e.getMessage());
                    }
                }
            }
        };
        this.__message_listener.start();
    }
    /*End Other Essential Methods*/
}
