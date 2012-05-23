package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import roge.gui.RWindow;
import roge.gui.border.StripedBorder;
import roge.net.ConnectionClient;
import roge.net.ConnectionClient.DataReceivedListener;
import roge.net.ConnectionClient.DataSendListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;
import roge.simplysecurechatclient.resources.Resources;


/*
 * TODO_HIGH:  The chatting system, as it is now, has an inherent flaw where if Client A were to send AFTER Client B sends but BEFORE 
 * client B's message is received, the key for decrypting the messages will be incorrect, and thus, unable to encode those messages
 * correctly.  Once those messages all messages are received, however, the system be back in sync, and messages will be received as
 * correctly.
 */

/**
 * Window allowing users to enter their session key and begin a chat session.
 * 
 * @author Nicholas Rogé
 */
public class SessionWindow extends RWindow implements DataReceivedListener,DataSendListener,SignalReceivedListener{
    /**For serialization*/
    private static final long serialVersionUID=-2916506936890995484L;
    
    private static final int ENCRYPTION_KEY_LENGTH=32;
    
    /**List of signals that this object may send.*/
    public static class Signals{
        /**Allows the server to seed the encryption key generator with a given value.*/
        public static class Seed extends Signal{
            private static final long serialVersionUID = -5747992139402358714L;
            
            private long __seed;
            
            
            /*Begin Constructors*/
            /**
             * Constructs this object.
             * 
             * @param seed Seed which will be used to seed the encryption key generator.
             */
            public Seed(long seed){
                this.__seed=seed;
            }
            /*End Constructors*/
            
            /*Begin Getter Methods*/
            /**
             * Gets the seed.
             * 
             * @return Returns the seed.
             */
            public long getSeed(){
                return this.__seed;
            }
            /*End Getter Methods*/
        }
    }

    /**Interface granting the ability to receive state change updates from this objects.*/
    public static interface SessionStateChangeListener{
        /**
         * Called when the SessionWindow object changes states.
         * 
         * @param window Window which changed states.
         * @param state_previous State of the window before the change occurred.
         * @param state_current State of the window after the state change.
         */
        public void onStateChange(SessionWindow window,SessionState state_previous,SessionState state_current);
    }
    
    /**List of possible window states*/
    public enum SessionState{
        /**Occurs when a session is waiting for new chat messages.*/
        AWAITING_CHAT_MESSAGE,
        /**Occurs when a hosting session successfully retrieves a host key and begins waiting for a client to connect to it.*/
        AWAITING_CONNECTION,
        /**Occurs when a hosting session begins waiting for a host key.*/
        AWAITING_HOST_KEY,
        /**Occurs when a chat message is received.*/
        CHAT_MESSAGE_RECEIVED,
        /**Occurs when the clients pair cannot be found.*/
        CONNECTION_FAILURE,
        /**Occurs when a session connects to another session.*/
        CONNECTION_SUCCESS,
        /**Occurs when a hosting session fails to receive its host key.*/
        HOST_KEY_RETRIEVAL_FAILED
    };
    
    private ChatPanel        __chat_panel;
    private SessionState     __current_status;
    private String           __encryption_key;
    private boolean          __is_host;
    private ConnectionClient __server_connection;
    private List<SessionStateChangeListener> __session_state_change_listeners;
    
    
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
    
    @Override public void onDataReceived(ConnectionClient client,Object data){
    }
    
    @Override public boolean onDataSend(ConnectionClient client,Object data){
        if(data instanceof Signal){
            if(data instanceof ChatMessage){                
                this.__encryptChatMessageSignal((ChatMessage)data);
                System.out.print("Encrypted message being sent:  "+((ChatMessage)data).getChatMessage()+"\n");
                return true;
            }
        }
        
        return true;
    }
    
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        if(signal instanceof ServerWindow.Signals.HostKeyResponse){
            this.__encryption_key=signal.getMessage();
        }else if(signal instanceof ServerWindow.Signals.ClientConnectionResponse){
            switch(signal.getMessageCode()){
                case ServerWindow.Signals.ClientConnectionResponse.CONNECTION_FAILURE:
                    this._changeState(SessionState.CONNECTION_FAILURE);
                    break;
                case ServerWindow.Signals.ClientConnectionResponse.CONNECTION_SUCCESS:
                    this._changeState(SessionState.CONNECTION_SUCCESS);
                    break;
            }
        }else if(signal instanceof Signals.Seed){
            this.regenerateEncryptionKey(((Signals.Seed)signal).getSeed());
        }else if(signal instanceof ChatMessage){
            this.__decryptChatMessageSignal((ChatMessage)signal);
            
            this._changeState(SessionState.CHAT_MESSAGE_RECEIVED);
            
            if(!((ChatMessage) signal).getSenderUsername().equals(ServerWindow.SERVER_USERNAME)){  //If the message is coming from the server, it doesn't have to be unencrypted.
                this.getChatPanel().receiveMessage((ChatMessage)signal);
            }
            
            this._changeState(SessionState.AWAITING_CHAT_MESSAGE);
        }
    }
    
    @Override public void windowClosing(WindowEvent event){
        if(this.__server_connection!=null){
            if(this.__server_connection.isConnected()){
                this.__server_connection.disconnect();
            }
        }
        
        System.exit(0);
    }
    /*End Overridden Methods*/
    
    /*Begin Getter Methods*/
    /**
     * Gets the chat panel for this window.
     * 
     * @return Returns the chat panel for this window.
     */
    public ChatPanel getChatPanel(){
        if(this.__chat_panel==null){
            if(this.__is_host){
                this.__chat_panel=new ChatPanel("Host");
            }else{
                this.__chat_panel=new ChatPanel("Client");
            }
        }
        
        return this.__chat_panel;
    }
    
    /**
     * Gets the currently active connection to the server.
     * 
     * @return Returns the currently active connection to the server.
     * 
     * @throws IOException Thrown if the connection could not be created.  Use the <code>getMessage</code> method on the caught exception for more information on the error.
     */
    protected ConnectionClient _getServerConnection() throws IOException{
        if(this.__server_connection==null){
            this.__server_connection=new ConnectionClient(Resources.Strings.server_host,Resources.Ints.server_port,true);
            
            this.__server_connection.setVerbose(Resources.Booleans.debugging);
            this.__server_connection.connect();
            this.__server_connection.addDataRecievedListener(this);
            this.__server_connection.addDataSendListener(this);
            this.__server_connection.addSignalListener(this);
        }

        return this.__server_connection;
    }
    
    /**
     * Gets the list of listeners to be called in the event of a state change of this window.
     * 
     * @return Returns the list of listeners to be called in the event of a state change of this window.
     */
    protected List<SessionStateChangeListener> getSessionStateChangeListeners(){
        if(this.__session_state_change_listeners==null){
            this.__session_state_change_listeners=new ArrayList<SessionStateChangeListener>();
        }
        
        return this.__session_state_change_listeners;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Adds a listener to be called when this window changes states.
     * 
     * @param listener Listener to be called when this window changes states.
     */
    public void addSessionStateChangeListener(SessionStateChangeListener listener){
        if(!this.getSessionStateChangeListeners().contains(listener)){
            this.getSessionStateChangeListeners().add(listener);
        }
    }
    
    /**
     * Broadcasts the state change to all of this window's listeners.
     * 
     * @param previous_state State of the window before the change occurred.
     * @param current_state State of the window after the change occurred.
     */
    protected void _broadcastSessionStateChange(final SessionState previous_state,final SessionState current_state){
        List<Thread> listener_threads=null;
        
       
        listener_threads=new ArrayList<Thread>();
        for(final SessionStateChangeListener listener:this.getSessionStateChangeListeners()){
            listener_threads.add(new Thread(new Runnable(){
                @Override public void run(){
                    listener.onStateChange(SessionWindow.this,previous_state,current_state);
                }
            }));
        }
        
        for(Thread thread:listener_threads){
            while(thread.isAlive()){ //Block while the any one of the threads is still running.
                try{
                    Thread.sleep(10);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Called to change this window's state.
     * 
     * @param state State to change to.
     */
    protected void _changeState(SessionState state){
        final SessionState previous_state=this.__current_status;
        
        
        this.__current_status=state;
        
        this._broadcastSessionStateChange(previous_state,state);
        this._onStateChanged(previous_state,state);
    }
    
    private final void __decryptChatMessageSignal(ChatMessage signal){
        char   current_character=0x00;
        String message=null;
        
        
        if(!signal.getSenderUsername().equals("SYSTEM")){  //System messages don't have to be decrypted.
            message=signal.getChatMessage();
            for(int encryption_key_index=this.__encryption_key.length()-1;encryption_key_index>-1;encryption_key_index--){
                for(int message_index=0;message_index<message.length();message_index++){
                    current_character=message.charAt(message_index);
                    
                    current_character=(char)((int)current_character^(int)this.__encryption_key.charAt(encryption_key_index));
                    message=message.substring(0,message_index)+current_character+message.substring(message_index+1,message.length());  //This just replaces the character we modified.
                }
            }
        }
        
        signal.setMessage(message);
    }
    
    /**
     * Allows the user to chat with the brother session to which this session is connected.
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _displayChatWindow(JPanel content){    
        this.setTitle(Resources.Strings.session_window_chatting_title);
        
        this._changeState(SessionState.AWAITING_CHAT_MESSAGE);
        this.setMinimumSize(new Dimension(400,600));
        this.setResizable(true);
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
            this.getChatPanel().setConnection(this.__server_connection);  //Server connection has to be initialized to be here.
            this.getChatPanel().getInputArea().setBorder(new StripedBorder(Color.WHITE,Color.BLACK));
            content.add(this.getChatPanel());
    }
    
    /**
     * Notifies the user that they failed to connect to the host/client
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _displayFailedConnect(JPanel content){
        JLabel failed_connect_display=null;
        
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
            failed_connect_display=new JLabel();
            failed_connect_display.setText(Resources.Strings.client_connection_failed);
            failed_connect_display.setForeground(Color.BLACK);
            content.add(failed_connect_display);
    }
    
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
            host_session.setBorder(new StripedBorder(Color.WHITE,Color.BLACK));
            host_session.setBackground(Color.WHITE);
            host_session.setForeground(Color.BLACK);
            host_session.addActionListener(new ActionListener(){
                @Override public void actionPerformed(ActionEvent arg0){
                    JPanel content=null;
                    
                    
                    content=new JPanel();
                    SessionWindow.this._displayHostKey(content);
                    
                    SessionWindow.this.changeContentPane(content);
                }
                
            });
            content.add(host_session);
        
            join_session=new JButton("Join Session");
            join_session.setBorder(new StripedBorder(Color.WHITE,Color.BLACK));
            join_session.setBackground(Color.WHITE);
            join_session.setForeground(Color.BLACK);
            join_session.addActionListener(new ActionListener(){
                @Override public void actionPerformed(ActionEvent event){
                    JPanel content=null;                    
                    
                    
                    content=new JPanel();
                    SessionWindow.this._displayJoinHost(content);
                    
                    SessionWindow.this.changeContentPane(content);
                }
             });
            content.add(join_session);
            
            
        this.setSize(new Dimension(300,200));
        this.setResizable(false);
    }
    
    /**
     * Displays the host key, allowing the host key to be given to someone wishing to join the session.
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _displayHostKey(JPanel content){
        final JLabel host_key_display=new JLabel();
        
        
        this.setTitle(Resources.Strings.session_window_hosting_session_title);
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
        
        
        new Thread(){
            @Override public void run(){
                long             host_key_retrieval_begin_time=0;  //Time when the host key retrieval began
                ConnectionClient server=null;
                
                try{
                    server=SessionWindow.this._getServerConnection();

                    server.send(new ServerWindow.Signals.HostKeyRequest());
                    
                    SessionWindow.this._changeState(SessionState.AWAITING_HOST_KEY);
                    host_key_retrieval_begin_time=System.currentTimeMillis();
                    while(true){  //Block until we have a host key, or we exceed the timeout
                        if(SessionWindow.this.__encryption_key==null){
                            if((System.currentTimeMillis()-host_key_retrieval_begin_time)>Resources.Ints.host_key_retrieval_timeout){  //If the time difference is greater than a second
                                throw new IOException(Resources.Strings.host_key_timeout_exceeded);
                            }
                        }else{
                            break;
                        }
                        
                        Thread.sleep(10);
                    }
                    
                    SessionWindow.this.__is_host=true;
                    host_key_display.setText(SessionWindow.this.__encryption_key);//Recall that if it gets to this point, no exceptions were thrown and a host key was retrieved successfully
                    
                    SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }catch(IOException e){                    
                    SessionWindow.this._changeState(SessionState.HOST_KEY_RETRIEVAL_FAILED);
                    host_key_display.setText("<html>"+String.format(Resources.Strings.host_key_retrieval_failed,e.getMessage())+"</html>");
                }
            }
        }.start();
        
        host_key_display.setText(Resources.Strings.retrieving_host_key);
        host_key_display.setForeground(Color.BLACK);
        content.add(host_key_display);
    }
    
    /**
     * Prompts the user to supply a key in order to connect to host session.
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _displayJoinHost(JPanel content){
        final JTextArea key=new JTextArea();
        
        JLabel  label=null;
        JButton connect=null;
        
        
        this.setTitle(Resources.Strings.session_window_joining_session_title);
        
        content.setLayout(new GridLayout(3,1));
        content.setBorder(new StripedBorder(Color.WHITE,Color.BLACK));
            label=new JLabel("Enter your host's key to connect:");
            content.add(label);
            
            content.add(key);
            
            connect=new JButton("Connect");
            connect.addActionListener(new ActionListener(){

                @Override public void actionPerformed(ActionEvent e){
                    ConnectionClient server=null;
                    
                    
                    try{
                        server=SessionWindow.this._getServerConnection();
                        server.connect();
                        
                        server.send(new ServerWindow.Signals.CreateSessionConnection(key.getText()));
                        
                        SessionWindow.this.__is_host=false;
                        SessionWindow.this.__encryption_key=key.getText();
                        SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                    }catch(IOException e1){
                        e1.printStackTrace();
                    }
                }
                
            });
            content.add(connect);
    }
    
    private final void __encryptChatMessageSignal(ChatMessage signal){
        char   current_character=0x00;
        String message=null;
        
        
        message=signal.getChatMessage();
        for(int encryption_key_index=0;encryption_key_index<this.__encryption_key.length();encryption_key_index++){
            for(int message_index=0;message_index<message.length();message_index++){
                current_character=message.charAt(message_index);
                
                current_character=(char)((int)current_character^(int)this.__encryption_key.charAt(encryption_key_index));
                message=message.substring(0,message_index)+current_character+message.substring(message_index+1,message.length());  //This just replaces the character we modified. 
            }
        }
        
        signal.setMessage(message);
    }
    
    /**
     * Method called when the state changes on this object.
     * 
     * @param previous_state State of the window before the change occurred.
     * @param current_state State of the window after the change occurred.
     */
    protected void _onStateChanged(SessionState previous_state,SessionState current_state){
        JPanel panel=null;
        
        switch(current_state){
            case CONNECTION_SUCCESS:
                this.regenerateEncryptionKey();
                
                panel=new JPanel();
                this._displayChatWindow(panel);
                this.changeContentPane(panel);
                break;
            case CONNECTION_FAILURE:
                panel=new JPanel();
                this._displayFailedConnect(panel);
                this.changeContentPane(panel);
                break;
            case CHAT_MESSAGE_RECEIVED:
                this.regenerateEncryptionKey();
                
                break;
        }
    }
    
    /**
     * Changes the encryption key.
     */
    
    private final void regenerateEncryptionKey(){
        char convert_me[]=null;
        long seed=0;
        
        
        if(this.__encryption_key.length()>=16){
            convert_me=this.__encryption_key.substring(0,16).toCharArray();
        }else{
            convert_me=this.__encryption_key.substring(0,this.__encryption_key.length()).toCharArray();
        }
        
        for(int index=0;index<convert_me.length;index++){
            seed+=convert_me[index]<<(index*8);
        }
        
        this.regenerateEncryptionKey(seed);
    }
    
    /**
     * Changes the encryption key, and seeds the number generator with the given parameter.
     * 
     * @param seed Seed for the random number generator.
     */
    private final void regenerateEncryptionKey(long seed){        
        int character=0;
        List<Character> character_pool=null;
        Random generator=null;
        String new_encryption_key=null;
 
                
        character_pool=new ArrayList<Character>();
        generator=new Random(seed);
            
        for(int index=0;index<SessionWindow.ENCRYPTION_KEY_LENGTH;index++){
            character_pool.add(new Character((char)generator.nextInt(256)));
        }
        
        for(int chars_left=character_pool.size();chars_left>0;chars_left--){
            character=Math.abs(generator.nextInt(character_pool.size()));
            
            new_encryption_key+=character_pool.get(character);
            character_pool.remove(character);
        }
        
        this.__encryption_key=new_encryption_key;
    }
    /*End Other Essential Methods*/
}

