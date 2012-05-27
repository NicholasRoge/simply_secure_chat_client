package roge.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import roge.simplysecurechatclient.gui.ChatWindow;
import roge.simplysecurechatclient.gui.ChatPanel.Signals.ChatMessage;

/**
 * Used to encrypt and decrypt data.
 * 
 * @author Nicholas Rogé
 */
public class DataEncryptor{
    /**Length the encryption string should be.  The longer the key is, the more secure it will be, at the cost of speed.*/
    public  static final int  ENCRYPTION_KEY_LENGTH=32;
    
    private List<Byte[]> __encryption_keys;
    
    
    /*Begin Constructors*/
    public DataEncryptor(long seed){
        this.__regenerateEncryptionKey(seed);
    }
    
    public DataEncryptor(String seed){
        
    }
    /*End Constructors*/
    
    /*Begin Other Essential Methods*/
    public final byte[] decryptData(byte[] data){
        byte   current_character=0x00;
        Byte[] encryption_key=null;
        int    encryption_index=0;
        int    message_index=0;
        byte[] unencrypted_data=null;
        int    wrap_count=0;
        
        
        unencrypted_data=new byte[data.length-3];
        System.arraycopy(data,3,unencrypted_data,0,data.length-3);
        wrap_count=data[0];
        encryption_index+=data[1]<<8;
        encryption_index+=data[2];
        
        encryption_key=this.__encryption_keys.get(encryption_index);
        message_index=unencrypted_data.length-1;
        for(int counter=0;counter<wrap_count;counter++){
            for(int encryption_key_index=encryption_key.length-1;encryption_key_index>-1;encryption_key_index--,message_index--){
                current_character=unencrypted_data[message_index];
                
                current_character=(byte)(current_character^encryption_key[encryption_key_index].byteValue());
                unencrypted_data[message_index]=current_character;
                
                if((unencrypted_data[message_index]-'0')!=(((message_index%10)+1)==10?0:((message_index%10)+1))){  //This will cause it to break (debug) if it deviates from the expected course.
                    System.out.println("Encryption Key During error:  "+encryption_key[encryption_key_index]);
                    System.out.println("What the fuck man...");
                }
                
                if(message_index==0){
                    message_index=unencrypted_data.length;
                }
            }
        }
        
        return unencrypted_data;
    }
    
    public final byte[] encryptData(byte[] data){
        byte    current_character=0x00;
        byte[]  encrypted_data=null;
        int     encryption_index=0;
        Byte[]  encryption_key=null;
        int     wrap_count=1;
        
        
        encryption_index=this.__encryption_keys.size()-1;
        encryption_key=this.__encryption_keys.get(encryption_index);
        encrypted_data=new byte[data.length+3];
        System.arraycopy(data,0,encrypted_data,3,data.length);           
        for(int encryption_key_index=0,message_index=3;(encryption_key_index!=encryption_key.length)||(message_index!=encrypted_data.length);encryption_key_index++,message_index++){
            if(message_index==encrypted_data.length){
                message_index=3;
            }
            
            if(encryption_key_index==encryption_key.length){
                wrap_count++;
                
                encryption_key_index=0;
            }
            
            current_character=encrypted_data[message_index];
            
            current_character=(byte)(current_character^encryption_key[encryption_key_index].byteValue());
            encrypted_data[message_index]=current_character; 
        }

        encrypted_data[0]=(byte)wrap_count;
        encrypted_data[1]=(byte)(encryption_index>>8);
        encrypted_data[2]=(byte)(encryption_index);
        return encrypted_data;
    }
    
    /**
     * Changes the encryption key.
     */
    public final void regenerateEncryptionKey(){
        byte convert_me[]=null;
        Byte[] previous_encryption_key=null;
        long seed=0;
        
        
        previous_encryption_key=this.__encryption_keys.get(this.__encryption_keys.size()-1);
        if(previous_encryption_key.length>=16){
            convert_me=new byte[16];
            for(int index=0;index<16;index++){
                convert_me[index]=previous_encryption_key[index].byteValue();
            }
        }else{
            convert_me=new byte[previous_encryption_key.length];
            for(int index=0;index<previous_encryption_key.length;index++){
                convert_me[index]=previous_encryption_key[index].byteValue();
            }
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
        List<Byte> byte_pool=null;
        Random generator=null;
        Byte[] new_encryption_key=null;
 
        
        if(this.__encryption_keys==null){
            this.__encryption_keys=new ArrayList<Byte[]>();
        }
                
        byte_pool=new ArrayList<Byte>();
        generator=new Random(seed);
            
        for(int index=0;index<DataEncryptor.ENCRYPTION_KEY_LENGTH;index++){
            byte_pool.add(new Byte((byte)generator.nextInt(256)));
        }
        
        new_encryption_key=new Byte[DataEncryptor.ENCRYPTION_KEY_LENGTH];
        for(int chars_left=byte_pool.size(),key_index=0;chars_left>0;chars_left--,key_index++){
            character=Math.abs(generator.nextInt(byte_pool.size()));
            
            new_encryption_key[key_index]=byte_pool.get(character);
            byte_pool.remove(character);
        }
        
        this.__encryption_keys.add(new_encryption_key);
    }
    /*End Other Essential Methods*/
}
