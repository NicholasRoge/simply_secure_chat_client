package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import roge.gui.RWindow;
import roge.gui.border.StripedBorder;
import roge.net.ConnectionClient;
import roge.net.ConnectionClient.DataSendListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;

/*
 * TODO_HIGH:  The chatting system, as it is now, has an inherent flaw where if Client A were to send AFTER Client B sends but BEFORE 
 * client B's message is received, the key for decrypting the messages will be incorrect, and thus, unable to encode those messages
 * correctly.  Once those messages all messages are received, however, the system be back in sync, and messages will be received as
 * correctly.
 */

/**
 * Window which displays the chat interface, allowing two clients to communicate.
 * 
 * @author Nicholas Rogé
 */
public class ChatWindow extends RWindow implements DataSendListener,SignalReceivedListener{
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
    
    /**Length the encryption string should be.  The longer the key is, the more secure it will be, at the cost of speed.*/
    public  static final int  ENCRYPTION_KEY_LENGTH=32;
    private static final long serialVersionUID = 467407181927209202L;

    private ChatPanel        __chat_panel;
    private String           __client_name;
    private String           __encryption_key;
    private ConnectionClient __server_connection;
    
    
    /*Begin Constructors*/
    /**
     * Constructs the object, giving the window no title.
     * 
     * @param server_connection Connection to the server through which this client is connected.  This connection should already be initialized and connected when it's passed into the constructor.
     * @param client_name This is the name that will be used when chatting with the other client.
     * @param encyrption_key_seed Seed to be used for when initially generating the encryption key.
     */
    public ChatWindow(ConnectionClient server_connection,String client_name,long encryption_key_seed){
        this("",server_connection,client_name,encryption_key_seed);
    }
    
    /**
     * Constructs the object, setting the window's title to the given string.
     * 
     * @param title Title for the window.
     * @param server_connection Connection to the server through which this client is connected.  This connection should already be initialized and connected when it's passed into the constructor.
     * @param client_name This is the name that will be used when chatting with the other client.
     * @param encyrption_key_seed Seed to be used for when initially generating the encryption key.
     */
    public ChatWindow(String title,ConnectionClient server_connection,String client_name,long encryption_key_seed){
        super(title);
        
        
        if(server_connection==null){
            throw new NullPointerException();
        }else{
            if(!server_connection.isConnected()){
                throw new IllegalArgumentException("The connection to the server must be initialized before it's used in the ChatWindow constructor.");
            }
        }
        if(client_name==null){
            throw new NullPointerException();
        }
        
        
        this.__client_name=client_name;
        this.__server_connection=server_connection;
        this.__server_connection.addDataSendListener(this);
        this.__server_connection.addSignalReceivedListener(this);
        
        this.__regenerateEncryptionKey(encryption_key_seed);
        
        this.setSize(new Dimension(400,600));
    }
    /*End Constructors*/
    
    /*Begin Overridden Methods*/
    @Override protected void _addContent(JPanel content){
        
        content.setBackground(Color.WHITE);
        content.setLayout(new GridLayout(1,1));
            this.__chat_panel=new ChatPanel(this.__client_name,this.__server_connection);
            this.getChatPanel().getInputArea().setBorder(new StripedBorder(Color.WHITE,Color.BLACK));
            content.add(this.getChatPanel());
    }
    
    @Override public boolean onDataSend(ConnectionClient client,Object data){
        if(data instanceof Signal){
            if(data instanceof ChatMessage){                
                this.__encryptChatMessageSignal((ChatMessage)data);

                return true;
            }
        }
        
        return true;
    }
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        if(signal instanceof ChatMessage){
            if(!((ChatMessage) signal).getSenderUsername().equals(ServerWindow.SERVER_USERNAME)){  //If the message is coming from the server, it doesn't have to be unencrypted.
                this.__decryptChatMessageSignal((ChatMessage)signal);
                
                this.__regenerateEncryptionKey();
            }
            
            this.getChatPanel().receiveMessage((ChatMessage)signal);
        }
    }
    
    @Override public void windowClosing(WindowEvent event){
        this.__server_connection.disconnect();
    }
    /*End Overridden Methods*/
    
    /*Begin Getter Methods*/
    /**
     * Gets the chat panel.
     * 
     * @return Gets the chat panel.
     */
    public ChatPanel getChatPanel(){
        return this.__chat_panel;
    }
    /*End Getter Methods*/
    
    /*Begin Other Essential Methods*/
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
     * Changes the encryption key.
     */
    private final void __regenerateEncryptionKey(){
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
        
        this.__regenerateEncryptionKey(seed);
    }
    
    /**
     * Changes the encryption key, and seeds the number generator with the given parameter.
     * 
     * @param seed Seed for the random number generator.
     */
    private final void __regenerateEncryptionKey(long seed){        
        int character=0;
        List<Character> character_pool=null;
        Random generator=null;
        String new_encryption_key=null;
 
                
        character_pool=new ArrayList<Character>();
        generator=new Random(seed);
            
        for(int index=0;index<ChatWindow.ENCRYPTION_KEY_LENGTH;index++){
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
