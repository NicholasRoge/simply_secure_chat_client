package roge.net;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Rogé
 */
public abstract class DataRecievedSubject{
    public static interface DataReceivedListener{
         public void receiveData(String data);
    }
    
    private List<DataReceivedListener> __listeners;
    
    
    public void addDataRecievedListener(DataReceivedListener listener){
        if(this.__listeners==null){
            this.__listeners=new ArrayList<DataReceivedListener>();
        }

        this.__listeners.add(listener);
    }
    
    public void broadcastData(String data){
        if(this.__listeners!=null){
            for(DataReceivedListener listener:this.__listeners){
                listener.receiveData(data);
            }
        }
    }
}
