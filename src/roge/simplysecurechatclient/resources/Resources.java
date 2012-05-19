package roge.simplysecurechatclient.resources;

@SuppressWarnings("javadoc")


/**Ideally, any resources which are used in more than one place, or are displayed to the user should be placed here for easy access.*/
public final class Resources{
    /**Integer Resources*/
    public final class Ints{
        /**Determines the maximum allowed time that a host will wait for a host key before timing out.  (Measured in milliseconds)*/
        public static final int host_key_retrieval_timeout=1000;
        public static final int server_port=1337;
    }
    
    /**Strings Resources*/
    public final class Strings{
        public static final String app_title="Simply Secure Chat Client";
        public static final String client_connection_failed="Failed to connect to the requested client.";
        public static final String no_host_with_given_key="No host could be found with that key.";
        public static final String host_key_retrieval_failed="Could not retrieve the host key.  Cause:<br />%s";
        public static final String host_key_timeout_exceeded="Timeout for key retrieval exceeded.";
        public static final String retrieving_host_key="Retrieving host key...  Please wait.";
        public static final String server_host="127.0.0.1";
        public static final String version="1.0.0";
    }
}
