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
import roge.security.DataEncryptor;
import roge.simplysecurechatclient.gui.ChatPanel.Message;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;


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
    private DataEncryptor    __encryptor;
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
        
        this.__encryptor=new DataEncryptor(encryption_key_seed);
        
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
                        JFileChooser file_chooser=null;
                        
                        
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
                            
                            try{
                                this.addExtensionIfNotExists(chosen_file,"txt");
                            }catch(IOException e){
                                return;  //If we couldn't add the extension...
                            }
                            
                            ChatWindow.this._exportChat(chosen_file);
                        }
                    }
                    
                    public void addExtensionIfNotExists(File file,String extension) throws IOException{
                        String filename=null;
                        int    last_period_index=-1;
                        
                                                
                        filename=file.getName();
                        
                        last_period_index=filename.lastIndexOf('.');
                        if(last_period_index==-1){                            
                            if(!file.renameTo(new File(file.toString()+"."+extension))){
                                throw new IOException("Could not add extension to filename.");
                            }
                        }else{
                            if(!filename.substring(last_period_index+1,filename.length()).equals(extension)){
                                if(last_period_index==filename.length()-1){  //In this case you have a file where the filename is something like "filename.".
                                    if(!file.renameTo(new File(file.toString()+extension))){
                                        throw new IOException("Could not add extension to filename.");
                                    }
                                }else{
                                    if(!file.renameTo(new File(file.toString()+"."+extension))){
                                        throw new IOException("Could not add extension to filename.");
                                    }
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
        ChatMessage cm_signal=null;
        byte[]      encrypted_byte_string=null;
        String      encrypted_message=null;
        
        
        if(data instanceof Signal){
            if(data instanceof ChatMessage){     
                cm_signal=(ChatMessage)data;
                
                /*
                 *  Begin Note
                 *    This had to be done to get around an error with the Java language.  Where the String(byte[]) constructor does not correctly convert all characters to their correct character representation.
                 */
                encrypted_byte_string=new byte[cm_signal.getChatMessage().length()];
                for(int index=0;index<cm_signal.getChatMessage().length();index++){
                    encrypted_byte_string[index]=(byte)cm_signal.getChatMessage().charAt(index);
                }
                
                encrypted_byte_string=this.__encryptor.encryptData(cm_signal.getChatMessage().getBytes());
                encrypted_message="";
                for(byte b:encrypted_byte_string){
                    encrypted_message+=(char)b;
                }
                /*
                 * End Note
                 */
                
                cm_signal.setChatMessage(encrypted_message);
                return true;
            }
        }
        
        return true;
    }
    
    @Override public void onSignalReceived(ConnectionClient client,Signal signal){
        ChatMessage cm_signal=null;
        byte[]      decrypted_byte_string=null;
        String      decrypted_message=null;
        
        
        if(signal instanceof ChatMessage){
            cm_signal=(ChatMessage)signal;
            
            if(!cm_signal.getSenderUsername().equals(ServerWindow.SERVER_USERNAME)){  //If the message is coming from the server, it doesn't have to be unencrypted.
                /*
                 *  Begin Note
                 *    This had to be done to get around an error with the Java language.  Where the String(byte[]) constructor does not correctly convert all characters to their correct character representation.
                 */
                decrypted_byte_string=new byte[cm_signal.getChatMessage().length()];
                for(int index=0;index<cm_signal.getChatMessage().length();index++){
                    decrypted_byte_string[index]=(byte)cm_signal.getChatMessage().charAt(index);
                }
                
                decrypted_byte_string=this.__encryptor.decryptData(decrypted_byte_string);
                decrypted_message="";
                for(byte b:decrypted_byte_string){
                    decrypted_message+=(char)b;
                }
                /*
                 * End Note
                 */
                
                cm_signal.setChatMessage(decrypted_message);
                this.__encryptor.regenerateEncryptionKey();
            }
            
            this.getChatPanel().receiveMessage(cm_signal);
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
    /*End Other Essential Methods*/
}
