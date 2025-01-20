import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    /* Runnable means this class can be passed to a thread pool and can be executed concurrently
        alongside other Runnable classes
     */

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server(){
        done = false;
        connections = new ArrayList<>();
    }

    @Override
    public void run(){
        try{
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done){
                // server has an accept method and returns a socket
                Socket client = server.accept();
                /* now opening an instance of ConnectionHandler because
                of needing to handle multiple clients */
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }
        catch(Exception e){
            shutdown();
        }
    }

    public void boradcast(String message){
        for(ConnectionHandler ch:connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            pool.shutdown();
            done = true;
            if(!server.isClosed()){
                server.close();
            }
            for(ConnectionHandler ch: connections){
                ch.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler (Socket client){
            this.client = client;
        }

        @Override
        public void run(){
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please Enter a Nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " connected");
                boradcast(nickname + "joined tha chat!");

                String message;
                while((message = in.readLine()) != null){
                    if(message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
                            boradcast(nickname + " has changed their nickname to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed nickname!!!");
                        }
                        else{
                            out.println("/nick enter_your_nickname_in_one_word!");
                        }
                    } else if (message.startsWith("/quit")) {
                        boradcast(nickname + " has left the chat:(");
                        shutdown();
                    } else {
                        boradcast(nickname + ": " + message);
                    }
                }
            }
            catch (IOException e){
                shutdown();
            }

        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try{
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
