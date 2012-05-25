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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import roge.gui.RWindow;
import roge.gui.border.StripedBorder;
import roge.net.ConnectionClient;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.Signal;
import roge.simplysecurechatclient.resources.Resources;




/**
 * Window allowing users to enter their session key and begin a chat session.
 * 
 * @author Nicholas Rogé
 */
public class SessionWindow extends RWindow implements SignalReceivedListener{
    /**For serialization*/
    private static final long serialVersionUID=-2916506936890995484L;

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
        /**Occurs when a hosting session successfully retrieves a host key and begins waiting for a client to connect to it.*/
        AWAITING_CONNECTION,
        /**Occurs when the clients pair cannot be found.*/
        CONNECTION_FAILURE,
        /**Occurs when a session connects to another session.*/
        CONNECTION_SUCCESS,
    };
    
    /**By default, the connection should be closed when this window is closed.  However, in the event that we're closing the window to open the ChatWindow, the connection should remain open.*/
    private boolean          __close_connection_on_exit;
    private SessionState     __current_status;
    private Long             __encryption_seed;
    private String           __host_key;
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
        
        this.__close_connection_on_exit=true;
    }
    /*End Constructors*/
    
    /*Overridden Methods*/
    @Override protected void _addContent(JPanel content){
        this._displayGetSessionTypePanel(content);
    } 
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        if(signal instanceof ServerWindow.Signals.HostKeyResponse){
            this.__host_key=signal.getMessage();
        }else if(signal instanceof ServerWindow.Signals.ClientConnectionResponse){
            switch(signal.getMessageCode()){
                case ServerWindow.Signals.ClientConnectionResponse.CONNECTION_FAILURE:
                    this._changeState(SessionState.CONNECTION_FAILURE);
                    break;
                case ServerWindow.Signals.ClientConnectionResponse.CONNECTION_SUCCESS:
                    this._changeState(SessionState.CONNECTION_SUCCESS);
                    break;
            }
        }else if(signal instanceof ChatWindow.Signals.Seed){
            this.__encryption_seed=new Long(((ChatWindow.Signals.Seed)signal).getSeed());
        }
    }
    
    @Override public void windowClosing(WindowEvent event){
        if(this.__close_connection_on_exit){
            if(this.__server_connection!=null){
                if(this.__server_connection.isConnected()){
                    this.__server_connection.disconnect();
                }
            }
        }
    }
    /*End Overridden Methods*/
    
    /*Begin Getter Methods*/    
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
            this.__server_connection.addSignalReceivedListener(this);
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
                    
                    SessionWindow.this.changeContentPane(content,true);
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
                    
                    SessionWindow.this.changeContentPane(content,true);
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
                    
                    host_key_retrieval_begin_time=System.currentTimeMillis();
                    while(true){  //Block until we have a host key, or we exceed the timeout
                        if(SessionWindow.this.__host_key==null){
                            if((System.currentTimeMillis()-host_key_retrieval_begin_time)>Resources.Ints.host_key_retrieval_timeout){  //If the time difference is greater than a second
                                throw new IOException(Resources.Strings.host_key_timeout_exceeded);
                            }
                        }else{
                            break;
                        }
                        
                        Thread.sleep(10);
                    }
                    
                    SessionWindow.this.__is_host=true;
                    host_key_display.setText(SessionWindow.this.__host_key);//Recall that if it gets to this point, no exceptions were thrown and a host key was retrieved successfully
                    
                    SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }catch(IOException e){                    
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
                        SessionWindow.this.__host_key=key.getText();
                        SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                    }catch(IOException e1){
                        e1.printStackTrace();
                    }
                }
                
            });
            content.add(connect);
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
                this._openChatWindow();
                
                break;
            case CONNECTION_FAILURE:
                panel=new JPanel();
                this._displayFailedConnect(panel);
                this.changeContentPane(panel,true);
                
                break;
        }
    }
    
    /**
     * Allows the user to chat with the brother session to which this session is connected.
     * 
     * @param content Panel which all components should be added to.
     */
    protected void _openChatWindow(){
        while(this.__encryption_seed==null){  //Block until we have an encryption key seed
            try{
                Thread.sleep(10);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        
        this.__server_connection.removeSignalReceivedListener(this);
        this.setVisible(false);
        this.__close_connection_on_exit=false;
        this.dispose();
        
        if(this.__is_host){
            new ChatWindow(Resources.Strings.session_window_chatting_title,this.__server_connection,"Host",this.__encryption_seed.longValue()).setVisible(true);
        }else{
            new ChatWindow(Resources.Strings.session_window_chatting_title,this.__server_connection,"Client",this.__encryption_seed.longValue()).setVisible(true);
        }
    }
    /*End Other Essential Methods*/
}

