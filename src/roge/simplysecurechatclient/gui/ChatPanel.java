package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SpringLayout;
import javax.swing.text.BadLocationException;
import roge.gui.widget.ETextArea;
import roge.net.ConnectionClient;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;

/**
 * Object which has an area for input, and an area which displays the messages that have been sent and received.
 * 
 * @author Nicholas Rog�
 */
public class ChatPanel extends JPanel{
    /**For serialization*/
    private static final long serialVersionUID = -7483751360235547132L;

    /**Signals that may be sent out by this object.*/
    public static class Signals{
        /**Signal to send a chat message between sessions.*/
        public static class ChatMessage extends Signal{
            private static final long serialVersionUID = 4302065424064019563L;
            
            private String __chat_message;
            private String __sender_username;
            private long   __server_timestamp;

            
            /*Begin Constructors*/
            /**
             * Constructs the signal to send a message between chat sessions.
             * 
             * @param message Chat message.
             * @param sender_username Name of chat client sending this message.  May be <code>null</code> to denote that it was generated by teh server.
             */
            public ChatMessage(String message,String sender_username){
                super();
                
                if(message==null){
                    throw new NullPointerException();
                }
                
                this.__chat_message=message;
                this.__sender_username=sender_username;
            }
            /*End Constructors*/
            
            /*Begin Getter Methods*/
            /**
             * Gets the chat message contents of this signal.
             * 
             * @return Returns the chat message contents of this signal.
             */
            public String getChatMessage(){
                return this.__chat_message;
            }
            
            /**
             * Gets the username of the ChatPanel who sent the message.
             * 
             * @return Returns the username of the ChatPanel who sent the message.
             */
            public String getSenderUsername(){
                return this.__sender_username;
            }
            
            /**
             * Gets the time that this message was sent from the server.
             * 
             * @return Returns the time that this message was sent from the server.
             */
            public long getServerTimestamp(){
                return this.__server_timestamp;
            }
            /*End Getter Methods*/
            
            /*Begin Setter Methods*/
            /**
             * Sets the chat message contents of the signal.
             * 
             * @param message Chat message contents.
             */
            public void setChatMessage(String message){
                if(message==null){
                    throw new NullPointerException();
                }
                
                this.__chat_message=message;
            }
            
            /**
             * Sets the time that this message was sent from the server.  Generally, this method should only be used by the server.
             * 
             * @param timestamp The time that this message was sent from the server.
             */
            public void setServerTimestamp(long timestamp){
                this.__server_timestamp=timestamp;
            }
            /*End Setter Methods*/
        }
    }
    
    /**
     * Contains all information about a message once it's retrieved by the ChatPanel object.
     * 
     * @author Nicholas Rog�
     */
    public static class Message{
        private String  __chat_username;
        private String  __message;
        private long    __timestamp;
        
        
        /*Begin Constructors*/
        /**
         * Constructs the message object.
         * 
         * @param message Message that this object contains.
         * @param timestamp Time the message went through the server.
         * @param chat_username Username of the message sender.
         */
        public Message(String message,long timestamp,String chat_username){
            this.__chat_username=chat_username;
            this.__message=message;
            this.__timestamp=timestamp;
        }
        /*End Constructors*/
        
        /*Begin Overridden Methods*/
        @Override public String toString(){
            final SimpleDateFormat formatter=new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss");
            
            return "["+this.__chat_username+"]["+formatter.format(new Date(this.__timestamp))+"]:  "+this.__message;
        }
        /*End Overridden Methods*/
        
        /*Begin Getter Methods*/
        /**
         * Gets this message contents.
         * 
         * @return Returns the message contents.
         */
        public String getMessage(){
            return this.__message;
        }
        
        /**
         * Gets the time this message went through the server.
         * 
         * @return Returns the time this message went through the server.
         */
        public long getTimestamp(){
            return this.__timestamp;
        }
        /*End Getter Methods*/
    }
    
    private String           __chat_username;
    private JTextPane        __chat_message_area;
    private JScrollPane      __chat_message_area_container;
    private List<Message>    __chat_messages;
    private ETextArea        __input_area;
    private ConnectionClient __server_connection;

    
    /*Begin Constructors*/
    /**
     * Constructs the ChatPanel
     * 
     * @param chat_username Sets the username for this chat client.
     * @param server_connection Already initialized and connected connection to use when sending messages to another chat session.  Will throw a <code>NullPointerException</code> if this parameter is null, or a <code>IllegalArgumentException</code> if the connection hasn't been initialized yet.
     */
    public ChatPanel(String chat_username,ConnectionClient server_connection){
        SpringLayout layout=null;
        
        
        if(server_connection==null){
            throw new NullPointerException();
        }else if(!server_connection.isConnected()){
            throw new IllegalArgumentException();
        }
        
        this.__chat_username=chat_username;
        this.__server_connection=server_connection;
        
        layout=new SpringLayout();
        this.setLayout(layout);

        this.__chat_message_area_container=new JScrollPane(this.getChatMessageArea(),JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.__chat_message_area_container.setAutoscrolls(true);
            this.getChatMessageArea().setEnabled(false);
            this.getChatMessageArea().setDisabledTextColor(Color.BLACK);
        this.add(this.__chat_message_area_container);
        layout.putConstraint(SpringLayout.NORTH,this.__chat_message_area_container,0,SpringLayout.NORTH,this);
        layout.putConstraint(SpringLayout.EAST,this.__chat_message_area_container,0,SpringLayout.EAST,this);
        layout.putConstraint(SpringLayout.WEST,this.__chat_message_area_container,0,SpringLayout.WEST,this);
        layout.putConstraint(SpringLayout.SOUTH,this.__chat_message_area_container,0,SpringLayout.NORTH,this.getInputArea());
              
        this.getInputArea().setLineWrap(true);
        this.getInputArea().setWrapStyleWord(true);
        this.getInputArea().setAutoscrolls(true);
        this.add(this.getInputArea());
        layout.putConstraint(SpringLayout.NORTH,this.getInputArea(),-80,SpringLayout.SOUTH,this);  //I don't think this is the proper way to do this, but it will have to do until I can figure out what the proper way is.
        layout.putConstraint(SpringLayout.EAST,this.getInputArea(),0,SpringLayout.EAST,this);
        layout.putConstraint(SpringLayout.WEST,this.getInputArea(),0,SpringLayout.WEST,this);
        layout.putConstraint(SpringLayout.SOUTH,this.getInputArea(),0,SpringLayout.SOUTH,this);
    }
    /*End Constructors*/
    
    /*Begin Getter Methods*/
    /**
     * Gets the username this chat client is set to.
     * 
     * @return Returns the username this chat client is set to.
     */
    public String getChatUsername(){
        return this.__chat_username;
    }
    
    /**
     * Gets the area all of the received and sent chat messages are shown.
     * 
     * @return The area all of the received and sent chat messages are shown.
     */
    public JTextPane getChatMessageArea(){
        if(this.__chat_message_area==null){
            this.__chat_message_area=new JTextPane();
        }
        
        return this.__chat_message_area;
    }
    
    /**
     * Gets the list of messages that have been sent.
     * 
     * @return The list of messages that have been sent.
     */
    public List<Message> getChatMessages(){
        if(this.__chat_messages==null){
            this.__chat_messages=new ArrayList<Message>();
        }
        
        return this.__chat_messages;
    }
    
    /**
     * Gets the input area of the chat panel.
     * 
     * @return The input area of the chat panel.
     */
    public ETextArea getInputArea(){
        if(this.__input_area==null){
            this.__input_area=new ETextArea(){
                private static final long serialVersionUID = -3150827064810838151L;

                
                /*Begin Overridden Methods*/
                @Override public void processKeyEvent(KeyEvent event){
                    if(event.getKeyCode()==KeyEvent.VK_ENTER){
                        if(!event.isShiftDown()){
                            switch(event.getID()){
                                case KeyEvent.KEY_PRESSED:
                                    if(isValidInput(ChatPanel.this.__input_area.getText())){
                                        try{
                                            if(ChatPanel.this.__server_connection==null){
                                                throw new IOException("Connection to the server has not yet been set.");
                                            }
                                            
                                            ChatPanel.this.__server_connection.send(new ChatMessage(ChatPanel.this.__input_area.getText(),ChatPanel.this.getChatUsername()));
                                            
                                            ChatPanel.this.__input_area.setText("");
                                        }catch(IOException e){
                                            e.printStackTrace();
                                        }
                                    }
                                case KeyEvent.KEY_RELEASED:
                                    return;
                            }
                        }
                    }
                    
                    super.processKeyEvent(event);
                }
                /*End Overridden Methods*/
                
                /*Begin Other Essential Methods*/
                /**
                 * Checks for valid input before sending it to the server.  Input is considered valid if it contains something other than whitespace within it.
                 * 
                 * @param input Text to be checked.
                 * 
                 * @return  Returns <code>true</code> if the input was valid, and <code>false</code> otherwise.
                 */
                protected boolean isValidInput(String input){
                    for(int index=0;index<input.length();index++){
                        if(input.charAt(index)>0x20){
                            return true;
                        }
                    }
                    
                    return false;
                }
                /*End Other Essential Methods*/
                
            };
        }
        
        return this.__input_area;
    }
    /*End Getter Methods*/
    
    /*Begin Setter Methods*/
    /**
     * Sets the username this chat client should be set to.
     * 
     * @param chat_username The username this chat client should be set to.
     */
    public void setChatUsername(String chat_username){
        this.__chat_username=chat_username;
    }
    /*End Setter Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Adds a message to the chat area.
     * 
     * @param message_signal Message to add.  Will throw a <code>NullPointerException</code> if this parameter is null.
     */
    public void receiveMessage(ChatMessage message_signal){
        int message_index=0;
        int message_text_begin_index=0;

        
        if(message_signal==null){
            throw new NullPointerException();
        }
        

        if(this.getChatMessages().size()==0){
            this.getChatMessages().add(new Message(message_signal.getChatMessage(),message_signal.getServerTimestamp(),message_signal.getSenderUsername()));
            
            message_index=0;
            message_text_begin_index=0;
        }else{
            for(int index=0;index<this.getChatMessages().size();index++){
                if(message_signal.getServerTimestamp()<this.getChatMessages().get(index).getTimestamp()){
                    this.getChatMessages().add(index,new Message(message_signal.getChatMessage(),message_signal.getServerTimestamp(),message_signal.getSenderUsername()));
                    message_index=index;
                    
                    break;
                }
                
                message_text_begin_index+=this.getChatMessages().get(index).toString().length()+1;  //The +1 is for the newline character that is added to it.
                
                if((index==this.getChatMessages().size()-1)){
                    this.getChatMessages().add(new Message(message_signal.getChatMessage(),message_signal.getServerTimestamp(),message_signal.getSenderUsername()));
                    message_index=index+1;
                    
                    break;
                }
            }
        }
        
        
        try{
            this.getChatMessageArea().getStyledDocument().insertString(message_text_begin_index,this.getChatMessages().get(message_index).toString()+"\n",null);
        }catch(BadLocationException e){
            //Theoretically, we should never get here.
            
            e.printStackTrace();
        }
    }
    /*End Other Essential Methods*/
}
