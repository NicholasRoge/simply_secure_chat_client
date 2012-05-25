/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import roge.net.ConnectionClient;
import roge.net.ConnectionClient.DataReceivedListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.ConnectionServer;
import roge.net.ConnectionServer.ClientConnectListener;
import roge.net.ConnectionServer.ClientDisconnectListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;
import roge.simplysecurechatclient.gui.ServerWindow.Signals.CreateSessionConnection;
import roge.simplysecurechatclient.resources.Resources;

/**
 * @author Nicholas Rogé
 *
 */
public class ServerWindow implements ClientConnectListener,DataReceivedListener,ClientDisconnectListener,SignalReceivedListener{
    /**Length of the host key in characters.*/
    public static final int    HOST_KEY_LENGTH=4;  //This gives 2^32 allowed waiting hosts
    /**Username that will be used in the event that the server sends out a ChatMessage signal.*/
    public static final String SERVER_USERNAME="SYSTEM";
    
    /**List of Signals present in this class.*/
    public static class Signals{
        /**Notification of a client disconnection.*/
        public static class ClientDisconnect extends Signal{
            private static final long serialVersionUID = -1406154262352471194L;
        }
        
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
    
    private Map<ConnectionClient,ConnectionClient> __clients;  //Where the value is the client
    private List<ConnectionClient>                 __connected_clients;
    private List<ConnectionClient>                 __hanging_clients;
    private Map<ConnectionClient,ConnectionClient> __hosts;  //Where the value is the host
    private ConnectionServer                       __server;
    private Map<String,ConnectionClient>           __waiting_hosts;
    
    /*Begin Constructors*/
    /**
     * Constructs the server.
     */
    public ServerWindow(){
        this.__server=new ConnectionServer(Resources.Ints.server_port);
        this.__server.setVerbose(Resources.Booleans.debugging);
        this.__server.start();
        
        this.__server.addClientConnectListener(this);
        this.__server.addClientDisconnectListener(this);
    }
    /*End Constructors*/
    
    /*Begin Overridden Methods*/
    @Override public boolean onClientConnect(ConnectionClient client){    
        client.addDataRecievedListener(this);
        client.addSignalReceivedListener(this);

        return true;
    }
    
    @Override public void onClientDisconnect(ConnectionClient client){
        ChatMessage signal=null;
        
                
        if(this.getWaitingHosts().containsValue(client)){
            for(Entry<String,ConnectionClient> entry:this.getWaitingHosts().entrySet()){
                if(entry.getValue().equals(client)){
                    this.getWaitingHosts().remove(entry.getKey());
                }
            }
        }else if(this.getClients().containsKey(client)){
            try{
                signal=new ChatMessage("The other session has disconnected.",ServerWindow.SERVER_USERNAME);
                signal.setServerTimestamp(System.currentTimeMillis());
                
                this.getBrotherSession(client).send(signal);
                this.getBrotherSession(client).send(new Signals.ClientDisconnect());
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(this.getHosts().containsKey(client)){
            try{
                signal=new ChatMessage("The other session has disconnected.",ServerWindow.SERVER_USERNAME);
                signal.setServerTimestamp(System.currentTimeMillis());
                
                this.getBrotherSession(client).send(signal);
                this.getBrotherSession(client).send(new Signals.ClientDisconnect());
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        
        this._removeClient(client);
        client.disconnect(false);
    }
    
    @Override public void onDataReceived(ConnectionClient client,Object data){
        //Ain't nobody here but us chickens.
    }
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        ConnectionClient connection=null;
        ChatMessage      chat_signal=null;
        String           host_key=null;
        long             seed=0;
        
        
        if(signal instanceof ServerWindow.Signals.HostKeyRequest){
            host_key=this._retrieveNewHostKey();
            
            try{
                client.send(new ServerWindow.Signals.HostKeyResponse(host_key));
                
                this.getAllConnectedClients().add(client);
                this.getWaitingHosts().put(host_key,client);
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(signal instanceof CreateSessionConnection){
            if(this.getWaitingHosts().containsKey(signal.getMessage())){
                connection=this.getWaitingHosts().get(signal.getMessage());
                
                this.getAllConnectedClients().add(client);  //This would be the first time the client has connected to this server.
                this.getClients().put(connection,client);
                this.getHosts().put(client,connection);
                
                this.getWaitingHosts().remove(signal.getMessage());
                
                try{
                    seed=new Random(System.currentTimeMillis()).nextLong();
                    
                    connection.send(new ChatWindow.Signals.Seed(seed));
                    client.send(new ChatWindow.Signals.Seed(seed));
                    
                    connection.send(new ServerWindow.Signals.ClientConnectionResponse(null,ServerWindow.Signals.ClientConnectionResponse.CONNECTION_SUCCESS));
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
            connection=this.getBrotherSession(client);
            
            try{
                if(connection==null){ //Recall that this means that the clients' brothers' chat session has been disconnected.
                    chat_signal=new ChatMessage("Message could not be sent.","SYSTEM");
                    chat_signal.setServerTimestamp(System.currentTimeMillis());
                    
                    client.send(chat_signal);
                }else{
                    this._processChatMessage((ChatMessage)signal);
                    
                    client.send(signal);
                    connection.send(signal);    
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    /*End Overridden Methods*/
    
    /*Begin Getter Methods*/
    public List<ConnectionClient> getAllConnectedClients(){
        if(this.__connected_clients==null){
            this.__connected_clients=new ArrayList<ConnectionClient>();
        }
        
        return this.__connected_clients;
    }
    
    /**
     * Gets the list of clients connected to this server.
     * 
     * @return Returns the list of clients connected to this server.
     */
    public Map<ConnectionClient,ConnectionClient> getClients(){
        if(this.__clients==null){
            this.__clients=new HashMap<ConnectionClient,ConnectionClient>();
        }
        
        return this.__clients;
    }
    
    public List<ConnectionClient> getHangingClientList(){
        if(this.__hanging_clients==null){
            this.__hanging_clients=new ArrayList<ConnectionClient>();
        }
        
        return this.__hanging_clients;
    }
    
    /**
     * Gets the list of hosts connected to this server.
     * 
     * @return Returns the list of hosts connected to this server.
     */
    public Map<ConnectionClient,ConnectionClient> getHosts(){
        if(this.__hosts==null){
            this.__hosts=new HashMap<ConnectionClient,ConnectionClient>();
        }
        
        return this.__hosts;
    }
    
    /**
     * Gets the list of hosts waiting for a client connection that are connected to this server.
     * 
     * @return Returns the list of hosts waiting for a client connection that are connected to this server.
     */
    public Map<String,ConnectionClient> getWaitingHosts(){
        if(this.__waiting_hosts==null){
            this.__waiting_hosts=new HashMap<String,ConnectionClient>();
        }
        
        return this.__waiting_hosts;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Gets the session that is connected to the given session.
     * 
     * @param brother Session to get the connected session of.
     * 
     * @return Returns the session that is connected to the given session.
     */
    protected ConnectionClient getBrotherSession(ConnectionClient brother){
        if(!this.getAllConnectedClients().contains(brother)||this.getHangingClientList().contains(brother)){ //Yeah...  Trying to get the brother of a session that isn't even connected to this server usually doesn't work very well.
            return null;
        }
        
        if(this.getHosts().containsKey(brother)){
            return this.getHosts().get(brother); 
        }else if(this.getClients().containsKey(brother)){
            return this.getClients().get(brother);
        }
        
        return null;
    }
    
    /**
     * Performs any necessary modifications to the chat signal that are necessary.
     * 
     * @param signal Chat message signal to process.
     */
    protected void _processChatMessage(ChatMessage signal){
        signal.setServerTimestamp(System.currentTimeMillis());
        
        //May add more here later.
    }
    
    protected void _removeClient(ConnectionClient client){
        if(this.getAllConnectedClients().contains(client)){
            if(this.getWaitingHosts().containsValue(client)){
                for(Entry<String,ConnectionClient> entry:this.getWaitingHosts().entrySet()){
                    if(entry.getValue().equals(client)){
                        this.getWaitingHosts().remove(entry.getKey());
                    }
                }
            }else if(this.getHosts().containsValue(client)){  //The client is a host, in this case.
                for(Entry<ConnectionClient,ConnectionClient> entry:this.getHosts().entrySet()){
                    if(entry.getValue().equals(client)){
                        this.getHosts().remove(entry.getKey());
                    }
                }
                this.getClients().remove(client);
                this.getAllConnectedClients().remove(client);
                
                this.getHangingClientList().add(this.getBrotherSession(client));
            }else if(this.getClients().containsValue(client)){
                for(Entry<ConnectionClient,ConnectionClient> entry:this.getClients().entrySet()){
                    if(entry.getValue().equals(client)){
                        this.getClients().remove(entry.getKey());
                    }
                }
                this.getHosts().remove(client);
                this.getAllConnectedClients().remove(client);
                
                this.getHangingClientList().add(this.getBrotherSession(client));
            }else if(this.getHangingClientList().contains(client)){
                this.getHangingClientList().remove(client);
            }else{
              //NOTE:  We should never get here.
            }
        }else{
            throw new RuntimeException("No such client was found."); 
        }
    }
    
    protected synchronized String _retrieveNewHostKey(){
        String host_key=null;
        Random generator=null;
        
        
        generator=new Random(System.currentTimeMillis());
        do{
            host_key="";
            for(int counter=0;counter<ServerWindow.HOST_KEY_LENGTH;counter++){
                host_key+=Integer.toHexString(generator.nextInt(16));
            }
        }while(this.getWaitingHosts().containsKey(host_key));  //Do this just to ensure there are no conflicts in teh hosts waiting list.
        
        return host_key.toUpperCase();
    }
    /*End Other Essential Methods*/
}
