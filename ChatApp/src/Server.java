import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    public ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try{
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }catch (Exception e){
            shutdown();
        }
    }

    public void broadcast(String message, ConnectionHandler sender){
        for(ConnectionHandler ch: connections){
            if(ch != null && ch != sender){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            done = true;
            pool.shutdown();
            if(!server.isClosed()){
                server.close();
            }
            for (ConnectionHandler ch: connections) {
                ch.shutdown();
            }
        }catch (IOException e){

        }
    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public ConnectionHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("PLease enter name: ");
                name = in.readLine();
                System.out.println(name + " connected!");
                broadcast(name + " joined!", this);
                String message;
                while ((message = in.readLine()) != null){
                    if(message.startsWith("/name")){
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
                            broadcast(name + " changed name to " + messageSplit[1], this);
                            System.out.println(name + " changed name to " + messageSplit[1]);
                            name = messageSplit[1];
                            out.println("Name changed successfully - " + name);
                        } else {
                            out.println("Write name please!");
                        }
                    } else if(message.equals("/quit")){
                        broadcast(name + " left the chat!", this);
                        shutdown();
                    } else{
                        broadcast(name + ": " + message, this);
                    }
                }
            }catch (IOException e ){
                shutdown();
            }
        }

        public void sendMessage(String message){
            try {
                out.println(message);
            } catch (Exception e) {

            }
        }

        public void shutdown(){
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }catch (IOException e){

            }
        }

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
