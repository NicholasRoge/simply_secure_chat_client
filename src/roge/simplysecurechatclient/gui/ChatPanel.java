/**
 * 
 */
package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import roge.gui.widget.ETextArea;
import roge.net.ConnectionClient;
import roge.simplysecurechatclient.gui.ServerWindow.Signals.ChatMessage;

/**
 * @author Nicholas Rogé
 *
 */
public class ChatPanel extends JPanel{
    public static class Message{
        private String __message;
        private long   __timestamp;
        
        
        /*Begin Constructors*/
        public Message(String message,long timestamp){
            this.__message=message;
            this.__timestamp=timestamp;
        }
        /*End Constructors*/
        
        /*Begin Overridden Methods*/
        @Override public String toString(){
            final SimpleDateFormat formatter=new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss");
            
            return "["+formatter.format(new Date(this.__timestamp))+"]:  "+this.__message;
        }
        /*End Overridden Methods*/
        
        /*Begin Getter Methods*/
        public String getMessage(){
            return this.__message;
        }
        
        public long getTimestamp(){
            return this.__timestamp;
        }
        
        public boolean isOrigin(){
            return true;
        }
        /*End Getter Methods*/
    }
    
    private JTextPane        __chat_message_area;
    private JScrollPane      __chat_message_area_container;
    private List<Message>    __chat_messages;
    private ETextArea        __input_area;
    private ConnectionClient __server;

    
    /*Begin Constructors*/
    /**
     * Constructs the ChatPanel
     */
    public ChatPanel(){
        GridBagConstraints constraints=null;
        
        
        this.setLayout(new GridLayout(2,1));
        constraints=new GridBagConstraints();
        constraints.fill=GridBagConstraints.BOTH;
        constraints.weightx=GridBagConstraints.REMAINDER;
        constraints.gridx=0;
            constraints.gridy=0;
            constraints.weighty=GridBagConstraints.REMAINDER;
            this.__chat_message_area_container=new JScrollPane(this.getChatMessageArea(),JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.__chat_message_area_container.setAutoscrolls(true);
                this.getChatMessageArea().setEnabled(false);
            this.add(this.__chat_message_area_container,constraints);
            
            constraints.gridy=1;
            constraints.weighty=0;
            this.getInputArea().setPreferredSize(new Dimension(0,80));
            this.getInputArea().setLineWrap(true);
            this.getInputArea().setWrapStyleWord(true);
            this.getInputArea().setAutoscrolls(true);
            this.add(this.getInputArea(),constraints);
            
        
        StyleConstants.setForeground(this.getChatMessageArea().addStyle("origin",null),Color.RED);
        StyleConstants.setForeground(this.getChatMessageArea().addStyle("!origin",null),Color.GRAY);
    }
    /*End Constructors*/
    
    /*Begin Getter Methods*/
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
                                            if(ChatPanel.this.__server==null){
                                                throw new IOException("Connection to the server has not yet been set.");
                                            }
                                            
                                            
                                            ChatPanel.this.__server.send(new ServerWindow.Signals.ChatMessage(ChatPanel.this.__input_area.getText()));
                                            
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
    
    public void setNotOriginTextColour(Color colour){
        StyleConstants.setForeground(this.getChatMessageArea().getStyle("!origin"),colour);
    }
    
    public void setOriginTextColour(Color colour){
        StyleConstants.setForeground(this.getChatMessageArea().getStyle("origin"),colour);
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
            this.getChatMessages().add(new Message(message_signal.getMessage(),message_signal.server_timestamp));
            
            message_index=0;
            message_text_begin_index=0;
        }else{
            for(int index=0;index<this.getChatMessages().size();index++){
                if(message_signal.server_timestamp<this.getChatMessages().get(index).getTimestamp()){
                    this.getChatMessages().add(index,new Message(message_signal.getMessage(),message_signal.server_timestamp));
                    message_index=index;
                    
                    break;
                }
                
                message_text_begin_index+=this.getChatMessages().get(index).toString().length()+1;  //The +1 is for the newline character that is added to it.
                
                if((index==this.getChatMessages().size()-1)){
                    this.getChatMessages().add(new Message(message_signal.getMessage(),message_signal.server_timestamp));
                    message_index=index+1;
                    
                    break;
                }
            }
        }
        
        
        
        try{
            if(true){
                this.getChatMessageArea().getStyledDocument().insertString(message_text_begin_index,this.getChatMessages().get(message_index).toString()+"\n",this.getChatMessageArea().getStyle("origin"));
            }else{
                this.getChatMessageArea().getStyledDocument().insertString(message_text_begin_index,this.getChatMessages().get(message_index).toString()+"\n",this.getChatMessageArea().getStyle("!origin"));
            }
        }catch(BadLocationException e){
            //Theoretically, we should never get here.
            
            e.printStackTrace();
        }
    }
    /*End Other Essential Methods*/
}
