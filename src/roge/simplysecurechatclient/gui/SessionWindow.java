/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import roge.net.ConnectionClient.DataReceivedListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ServerWindow.Signals.ChatMessage;
import roge.simplysecurechatclient.resources.Resources;

/**
 * Window allowing users to enter their session key and begin a chat session.
 * 
 * @author Nicholas Rogé
 */
public class SessionWindow extends RWindow implements DataReceivedListener,SignalReceivedListener{
    public static interface SessionStateChangeListener{
        public void onStateChange(SessionWindow window,SessionState state_previous,SessionState state_current);
    }
    
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
    
    private String           __chat_message;
    private ChatPanel        __chat_panel;
    private SessionState     __current_status;
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
    }
    /*End Constructors*/
    
    /*Overridden Methods*/
    @Override protected void _addContent(JPanel content){
        this._displayGetSessionTypePanel(content);
    }
    
    /**
     * 
     * @param client This will ALWAYS be the __server_connection member.
     */
    @Override public void onDataReceived(ConnectionClient client,Object data){
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
        }else if(signal instanceof ChatMessage){
            this.getChatPanel().receiveMessage((ChatMessage)signal);
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
    public ChatPanel getChatPanel(){
        if(this.__chat_panel==null){
            this.__chat_panel=new ChatPanel();  
        }
        
        return this.__chat_panel;
    }
    
    protected ConnectionClient _getServerConnection() throws IOException{
        if(this.__server_connection==null){
            this.__server_connection=new ConnectionClient(Resources.Strings.server_host,Resources.Ints.server_port,true);
            
            this.__server_connection.connect();
            this.__server_connection.addDataRecievedListener(this);
            this.__server_connection.addSignalListener(this);
        }

        return this.__server_connection;
    }
    
    protected List<SessionStateChangeListener> getSessionStateChangeListeners(){
        if(this.__session_state_change_listeners==null){
            this.__session_state_change_listeners=new ArrayList<SessionStateChangeListener>();
        }
        
        return this.__session_state_change_listeners;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
    public void addSessionStateChangeListener(SessionStateChangeListener listener){
        if(!this.getSessionStateChangeListeners().contains(listener)){
            this.getSessionStateChangeListeners().add(listener);
        }
    }
    
    protected void _broadcastSessionStateChange(SessionState previous_state,SessionState current_state){
        for(SessionStateChangeListener listener:this.getSessionStateChangeListeners()){
            listener.onStateChange(this,previous_state,current_state);
        }
    }
    
    protected void _changeState(SessionState state){
        final SessionState previous_state=this.__current_status;
        
        
        this.__current_status=state;
        
        this._broadcastSessionStateChange(previous_state,state);
        this._onStateChanged(previous_state,state);
    }
    
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
    
    protected void _displayHostKey(JPanel content){
        final JLabel host_key_display=new JLabel();
        
        
        this.setTitle(Resources.Strings.session_window_hosting_session_title);
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
        
        this.__is_host=true;
        
        new Thread(){
            @Override public void run(){
                long             host_key_retrieval_begin=0;  //Time when the host key retrieval began
                ConnectionClient server=null;
                
                try{
                    server=SessionWindow.this._getServerConnection();

                    server.send(new ServerWindow.Signals.HostKeyRequest());
                    
                    SessionWindow.this._changeState(SessionState.AWAITING_HOST_KEY);
                    host_key_retrieval_begin=System.currentTimeMillis();
                    while(true){  //Block until we have a host key, or we exceed the timeout
                        if(SessionWindow.this.__host_key==null){
                            if((System.currentTimeMillis()-host_key_retrieval_begin)>Resources.Ints.host_key_retrieval_timeout){  //If the time difference is greater than a second
                                throw new IOException(Resources.Strings.host_key_timeout_exceeded);
                            }
                        }else{
                            break;
                        }
                        
                        Thread.sleep(100);  //TODO_HIGH:  Look into why this changes this loop.  Without the sleep, the retrieval always seems to timeout, even when the host_key clearly arrives.
                    }
                    
                    SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                    host_key_display.setText(SessionWindow.this.__host_key);//Recall that if it gets to this point, no exceptions were thrown and a host key was retrieved successfully
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
                        
                        SessionWindow.this._changeState(SessionState.AWAITING_CONNECTION);
                    }catch(IOException e1){
                        e1.printStackTrace();
                    }
                }
                
            });
            content.add(connect);
    }
    
    protected void _onStateChanged(SessionState previous_state,SessionState current_state){
        JPanel panel=null;
        
        switch(current_state){
            case CONNECTION_SUCCESS:                
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
                System.out.print(this.__chat_message);
                break;
        }
    }
    /*End Other Essential Methods*/
}

