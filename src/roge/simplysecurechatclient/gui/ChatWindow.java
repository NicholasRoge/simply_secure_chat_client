package roge.simplysecurechatclient.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import roge.gui.RWindow;
import roge.gui.border.StripedBorder;
import roge.net.ConnectionClient;
import roge.net.ConnectionClient.DataSendListener;
import roge.net.ConnectionClient.SignalReceivedListener;
import roge.net.Signal;
import roge.simplysecurechatclient.gui.ChatPanel.Message;
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
    
    @Override protected void _addMenu(JFrame frame){
        JMenuBar menu_bar=null;
        JMenu menu=null;
        JMenuItem menu_item=null;
        
        menu_bar=new JMenuBar();
            menu=new JMenu("File");
            menu.setMnemonic('f');
                menu_item=new JMenuItem("Export Conversation");
                menu_item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,KeyEvent.CTRL_MASK));
                menu_item.addActionListener(new ActionListener(){
                    @Override public void actionPerformed(ActionEvent event){
                        File         chosen_file=null;
                        String       extension=null;
                        JFileChooser file_chooser=null;
                        FileOutputStream file_stream=null;
                        
                        
                        file_chooser=new JFileChooser();
                        file_chooser.setFileFilter(new FileFilter(){
                            @Override public boolean accept(File file){
                                String extension=null;
                                
                                
                                if(file.isDirectory()){
                                    return true;
                                }
                                
                                extension=file.getName();
                                if(extension.lastIndexOf('.')==-1){
                                    return false;  //Filter the file out if it has no extension.
                                }
                                
                                extension=extension.substring(extension.lastIndexOf('.')+1,extension.length());
                                if(extension.equals("txt")){
                                    return true;
                                }else{
                                    return false;
                                }
                            }
                            
                            @Override public String getDescription(){
                                return "*.txt";
                            }
                            
                        });
                        
                        if(file_chooser.showSaveDialog(ChatWindow.this)==JFileChooser.APPROVE_OPTION){
                            chosen_file=file_chooser.getSelectedFile();
                            
                            this.addExtensionIfNotExists(chosen_file,"txt");
                            
                            ChatWindow.this._exportChat(chosen_file);
                        }
                    }
                    
                    public void addExtensionIfNotExists(File file,String extension){
                        String filename=null;
                        int    last_period_index=-1;
                        
                        
                        filename=file.getName();
                        
                        last_period_index=filename.lastIndexOf('.');
                        if(last_period_index==-1){
                            file.renameTo(new File(filename+"."+extension));
                        }else{
                            if(!filename.substring(last_period_index+1,filename.length()).equals(extension)){
                                if(last_period_index==filename.length()-1){  //In this case you have a file where the filename is something like "filename.".
                                    file.renameTo(new File(filename+extension));
                                }else{
                                    file.renameTo(new File(filename+"."+extension));
                                }
                            }
                        }
                        
                        return;
                    }
                });
                menu.add(menu_item);
                
                menu.addSeparator();
                
                menu_item=new JMenuItem("Exit");
                menu_item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,KeyEvent.ALT_MASK));
                menu_item.addActionListener(new ActionListener(){
                    @Override public void actionPerformed(ActionEvent event){
                        ChatWindow.this.setVisible(false);
                        ChatWindow.this.dispose();
                    }
                });
                menu.add(menu_item);
            menu_bar.add(menu);
        frame.setJMenuBar(menu_bar);
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
        int    message_index=0;
        int    wrap_number=0;
        
        
        message=signal.getChatMessage();
        wrap_number=message.charAt(0);
        message=message.substring(1,message.length());//Gotta get rid of the first wrap amount character.
        message_index=message.length()-1;
        
        for(int counter=0;counter<wrap_number;counter++){
            for(int encryption_key_index=this.__encryption_key.length()-1;encryption_key_index>-1;encryption_key_index--,message_index--){
                current_character=message.charAt(message_index);
                
                current_character=(char)((int)current_character^(int)this.__encryption_key.charAt(encryption_key_index));
                message=message.substring(0,message_index)+current_character+message.substring(message_index+1,message.length());  //This just replaces the character we modified.
                
                if(message_index==0){
                    message_index=message.length();
                }
            }
        }
        
        signal.setMessage(message);
    }
    
    private final void __encryptChatMessageSignal(ChatMessage signal){
        char    current_character=0x00;
        String  encrypted_message=null;
        int     wrap_count=1;
        
        
        encrypted_message=signal.getChatMessage();            
        for(int encryption_key_index=0,message_index=0;(encryption_key_index!=this.__encryption_key.length())||(message_index!=encrypted_message.length());encryption_key_index++,message_index++){
            if(message_index==encrypted_message.length()){
                message_index=0;
            }
            
            if(encryption_key_index==this.__encryption_key.length()){
                wrap_count++;
                
                encryption_key_index=0;
            }
            
            current_character=encrypted_message.charAt(message_index);
            
            current_character=(char)((int)current_character^(int)this.__encryption_key.charAt(encryption_key_index));
            encrypted_message=encrypted_message.substring(0,message_index)+current_character+encrypted_message.substring(message_index+1,encrypted_message.length());  //This just replaces the character we modified. 
        }
        
        signal.setMessage((char)wrap_count+encrypted_message);
    }
    
    protected void _exportChat(File file){
        FileOutputStream output_stream=null;
        
        
        try{
            output_stream=new FileOutputStream(file);
        
            for(Message message:ChatWindow.this.getChatPanel().getChatMessages()){
                for(char character:(message.toString()+System.getProperty("line.separator")).toCharArray()){
                    output_stream.write(character);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
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
