/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import roge.net.ConnectionClient;
import roge.net.ConnectionClient.DataReceivedListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.ConnectionServer;
import roge.net.ConnectionServer.ClientConnectListener;
import roge.net.ConnectionServer.ClientDisconnectListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;
import roge.simplysecurechatclient.resources.Resources;

/**
 * @author Nicholas Rogé
 *
 */
public class ServerWindow implements ClientConnectListener,DataReceivedListener,ClientDisconnectListener,SignalReceivedListener{
    public static final String CLIENT_DISCONNECTED="client_disconnected";
    /**List of Signals present in this class.*/
    public static class Signals{        
        /**Response signal containing the host key.*/
        public static class HostKeyResponse extends Signal{
            private static final long serialVersionUID = 2610575189906570480L;
            
            
            /*Begin Constructors*/
            /**
             * Constructs the object where the message is the requesting client's host key.
             * 
             * @param message The requesting clients host key.  If this is null, a <code>NullPointerException</code> will be thrown.
             */
            public HostKeyResponse(String message){
                super(message);
                
                if(message==null){
                    throw new NullPointerException();
                }
            }
            /*End Constructors*/
        }

        /**Signal to request a host key.*/
        public static class HostKeyRequest extends Signal{
            private static final long serialVersionUID = 2873755714066337676L;
        }

        /**Signal to give the status of another client connecting to the session.*/
        public static class ClientConnectionResponse extends Signal{
            private static final long serialVersionUID = -6058937927689798155L;
            
            /**Client connected unsuccessfully.*/
            public static final int CONNECTION_FAILURE=-1;
            /**Client connected successfully.*/
            public static final int CONNECTION_SUCCESS=1;
            
            
            /*Begin Constructors*/
            /**
             * Constructs the signal to be sent.
             * 
             * @param message This message cannot be null if the message code indicator is CONNECTION_FAILURE.  If it is, it will throw a <code>NullPointerException</code> 
             * @param message_code Should be one of CONNECTION_FAILURE or CONNECTION_SUCCESS.  If this parameter is null, a <code>NullPointerException</code> will be thrown.
             */
            public ClientConnectionResponse(String message,Integer message_code){
                super(message,message_code);
                
                if(message_code==null){
                    throw new NullPointerException();
                }else if(message_code==ClientConnectionResponse.CONNECTION_FAILURE){
                    if(message==null){
                        throw new NullPointerException();
                    }
                }
            }
        }
        
        /**Signal to create a connection between two sessions.*/
        public static class CreateSessionConnection extends Signal{
            private static final long serialVersionUID = 6094834124698092023L;

            
            /*Begin Constructors*/
            /**
             * Constructs the signal to create a connection between two sessions.
             * 
             * @param message This is the host key for the host the session wishes to connect to.  If this parameter is null, a <code>NullPointerException</code> will be thrown.
             */
            public CreateSessionConnection(String message){
                super(message);
            }
            /*End Constructors*/
        }
    }
    
    private Map<ConnectionClient,ConnectionClient> __clients;  //Where the key is the client
    private Map<ConnectionClient,ConnectionClient> __hosts;  //Where the key is the host
    private ConnectionServer __server;
    private Map<String,ConnectionClient> __waiting_hosts;
    
    /*Begin Constructors*/
    public ServerWindow(){
        this.__server=new ConnectionServer(Resources.Ints.server_port);
        this.__server.start();
        
        this.__server.addClientConnectListener(this);
        this.__server.addClientDisconnectListener(this);
    }
    /*End Constructors*/
    
    /*Begin Overridden Methods*/
    @Override public boolean onClientConnect(ConnectionClient client){
        client.addDataRecievedListener(this);
        client.addSignalListener(this);

        return true;
    }
    
    @Override public void onClientDisconnect(ConnectionClient client){
        System.out.println("Client disconnected.");
        
        if(this.getWaitingHosts().containsValue(client)){
            for(Entry entry:this.getWaitingHosts().entrySet()){
                if(entry.getValue().equals(client)){
                    this.getWaitingHosts().remove(entry.getKey());
                }
            }
        }else if(this.getClients().containsKey(client)){
            try{
                this.getHosts().get(this.getClients().get(client)).send(ServerWindow.CLIENT_DISCONNECTED);
            }catch(IOException e){
                e.printStackTrace();
            }
            
            this.getHosts().remove(this.getClients().get(client));
            this.getClients().remove(client);
        }else if(this.getHosts().containsKey(client)){
            try{
                this.getClients().get(this.getHosts().get(client)).send(ServerWindow.CLIENT_DISCONNECTED);
            }catch(IOException e){
                e.printStackTrace();
            }
            
            this.getClients().remove(this.getHosts().get(client));
            this.getHosts().remove(client);
        }
    }
    
    @Override public void onDataReceived(ConnectionClient client,Object data){
        
    }
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        ConnectionClient host=null;
        
        
        if(signal instanceof ServerWindow.Signals.HostKeyRequest){
            try{
                client.send(new ServerWindow.Signals.HostKeyResponse("12345"));
                
                this.getWaitingHosts().put("12345",client);
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(signal instanceof ServerWindow.Signals.CreateSessionConnection){
            System.out.print(this.getWaitingHosts().containsKey(signal.getMessage())+":"+signal.getMessage());
            if(this.getWaitingHosts().containsKey(signal.getMessage())){
                host=this.getWaitingHosts().get(signal.getMessage());
                
                this.getClients().put(host,client);
                this.getHosts().put(client,host);
                
                this.getWaitingHosts().remove(signal.getMessage());
                
                try{
                    host.send(new ServerWindow.Signals.ClientConnectionResponse(null,ServerWindow.Signals.ClientConnectionResponse.CONNECTION_SUCCESS));
                    client.send(new ServerWindow.Signals.ClientConnectionResponse(null,ServerWindow.Signals.ClientConnectionResponse.CONNECTION_SUCCESS));
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else{
                try{
                    client.send(new ServerWindow.Signals.ClientConnectionResponse(Resources.Strings.no_host_with_given_key,ServerWindow.Signals.ClientConnectionResponse.CONNECTION_FAILURE));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }else if(signal instanceof ChatMessage){
            if(this.getHosts().containsKey(client)||this.getClients().containsKey(client)){
                try{
                    System.out.print("Receieved message from "+((ChatMessage)signal).getSender()+"\n");
                    this._processChatMessage((ChatMessage)signal);
                    
                    client.send(signal);
                    this.getBrotherSession(client).send(signal);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else{
                //Right now we're just consuming the message if it can't be sent.
            }
        }
    }
    /*End Overridden Methods*/
    
    /*Begin Getter Methods*/
    public Map<ConnectionClient,ConnectionClient> getClients(){
        if(this.__clients==null){
            this.__clients=new HashMap<ConnectionClient,ConnectionClient>();
        }
        
        return this.__clients;
    }
    
    public Map<ConnectionClient,ConnectionClient> getHosts(){
        if(this.__hosts==null){
            this.__hosts=new HashMap<ConnectionClient,ConnectionClient>();
        }
        
        return this.__hosts;
    }
    
    public Map<String,ConnectionClient> getWaitingHosts(){
        if(this.__waiting_hosts==null){
            this.__waiting_hosts=new HashMap<String,ConnectionClient>();
        }
        
        return this.__waiting_hosts;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
    protected ConnectionClient getBrotherSession(ConnectionClient brother){
        if(this.getHosts().containsKey(brother)){
            return this.getHosts().get(brother); 
        }else if(this.getClients().containsKey(brother)){
            return this.getClients().get(brother);
        }else{
            return null;
        }
    }
    
    protected void _processChatMessage(ChatMessage signal){
        signal.setServerTimestamp(System.currentTimeMillis());
        
        //May add more here later.
    }
    /*End Other Essential Methods*/
}
