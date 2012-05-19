/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import roge.gui.widget.ETextArea;
import roge.net.ConnectionClient;

/**
 * @author Nicholas Rogé
 *
 */
public class ChatPanel extends JPanel{
    private JTextArea        __chat_message_area;
    private List<String>     __chat_messages;
    private ETextArea        __input_area;
    private ConnectionClient __server;

    
    /*Begin Constructors*/
    /**
     * Constructs the ChatPanel
     */
    public ChatPanel(){
        GridBagConstraints constraints=null;
        
        
        this.setLayout(new GridLayout(2,1));
            this.getChatMessageArea().setEnabled(false);
            this.add(this.getChatMessageArea(),constraints);
            
            this.add(this.getInputArea(),constraints);
    }
    /*End Constructors*/
    
    /*Begin Getter Methods*/
    /**
     * Gets the area all of the received and sent chat messages are shown.
     * 
     * @return The area all of the received and sent chat messages are shown.
     */
    public JTextArea getChatMessageArea(){
        if(this.__chat_message_area==null){
            this.__chat_message_area=new JTextArea();
        }
        
        return this.__chat_message_area;
    }
    
    /**
     * Gets the list of messages that have been sent.
     * 
     * @return The list of messages that have been sent.
     */
    public List<String> getChatMessages(){
        if(this.__chat_messages==null){
            this.__chat_messages=new ArrayList<String>();
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

                @Override public void processKeyEvent(KeyEvent event){
                    if(event.getKeyCode()==KeyEvent.VK_ENTER){
                        if(!event.isShiftDown()){
                            switch(event.getID()){
                                case KeyEvent.KEY_PRESSED:
                                    try{
                                        if(ChatPanel.this.__server==null){
                                            throw new IOException("Connection to the server has not yet been set.");
                                        }
                                        
                                        
                                        ChatPanel.this.addMessage(ChatPanel.this.__input_area.getText());
                                        ChatPanel.this.__server.send(new ServerWindow.Signals.ChatMessage(ChatPanel.this.__input_area.getText()));
                                        
                                        ChatPanel.this.__input_area.setText("");
                                    }catch(IOException e){
                                        e.printStackTrace();
                                    }
                                case KeyEvent.KEY_RELEASED:
                                    return;
                            }
                        }
                    }
                    
                    super.processKeyEvent(event);
                }
                
            };
        }
        
        return this.__input_area;
    }
    /*End Getter Methods*/
    
    /*Begin Setter Methods*/
    /**
     * Sets the connection to use when sending messages to another chat session.
     * 
     * @param connection Already initialized and connected connection to use when sending messages to another chat session.  Will throw a <code>NullPointerException</code> if this parameter is null, or a <code>IllegalArgumentException</code> if the connection hasn't been initialized yet.  
     */
    public void setConnection(ConnectionClient connection){
        if(connection==null){
            throw new NullPointerException();
        }else if(!connection.isConnected()){
            throw new IllegalArgumentException();
        }
        
        this.__server=connection;
    }
    /*End Setter Methods*/
    
    /*Begin Other Essential Methods*/
    /**
     * Adds a message to the chat area.
     * 
     * @param message Message to add.  Will throw a <code>NullPointerException</code> if this parameter is null.
     */
    public void addMessage(String message){
        String full_message=null;
        
        
        if(message==null){
            throw new NullPointerException();
        }
        
        this.getChatMessages().add(message);
        
        full_message="";
        for(String chat_message:this.getChatMessages()){
            full_message+=chat_message+"\n";
        }
        
        this.getChatMessageArea().setText(full_message);
    }
    /*End Other Essential Methods*/
}
