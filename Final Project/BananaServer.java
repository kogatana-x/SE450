import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/* Name: BananaServer
 * Description: The main class for running the site
 * Parameters: none
 * Relationships: none
 */

public class BananaServer {
    private static final int PORT = 8080;
    public HashMap<String,User> Sessions = new HashMap<String, User>(); //TODO definitley a security risk but i give no fucks right now

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                //System.out.println("New client connected");

                executorService.execute(new ClientHandler(socket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    private String getSourceInfo(Socket socket){ //TODO Logger
        String ip = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        return ip + ":" + port;
    }
    private String getRequestInfo(BufferedReader reader) throws IOException{
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }
   
    @Override
    public void run(){

        HTMLParser parser = new HTMLParser(socket);
        parser.parseRawStrings();
        String method=parser.getMethod();
        String path=parser.getPath();

        parser.getValues();
       // String cookie = parser.getCookie();

        String filename="garbage";
        String mimeType = "text/html";

        boolean result=false;

        if(method.equals("POST")&&path.equals("/register")){
            String username="",password="",firstname="",lastname="";
            for (int x=0;x<parser.values.length;x++) {
                if (parser.values[x]==null){break;}
                else if (parser.values[x].equals("username")) {
                    x++;
                    username = parser.values[x];
                } else if (parser.values[x].equals("password")) {
                    x++;
                    password = parser.values[x];
                } else if (parser.values[x].equals("firstname")) {
                    x++;
                    firstname = parser.values[x];
                } else if (parser.values[x].equals("lastname")) {
                    x++;
                    lastname = parser.values[x];
                }
           }
           UserAuthenticator authenticator = new UserAuthenticator(new UserRepository());
           result=authenticator.register(username,password,firstname,lastname);
           String res;
           if(result){res="success";}else{res="failure";}
           System.out.println("Registration attempt "+res+" from " + getSourceInfo(socket) + " with username " + username);
           filename="finished-registration.html"; //TODO add a page for failed login and update username if logged in
           //NOTE: does not auto-login with new user account
        } 
        
        else if(method.equals("POST")&&path.equals("/login")){
            String username="",password="";
            for (int x=0;x<parser.values.length;x++) {
                if (parser.values[x]==null){break;}
                else if (parser.values[x].equals("username")) {
                    x++;
                    username = parser.values[x];
                } else if (parser.values[x].equals("password")) {
                    x++;
                    password = parser.values[x];
                } 
           }
           UserAuthenticator authenticator = new UserAuthenticator(new UserRepository());
           String res;
           result=authenticator.login(username, password);
           if(result){res="success";}else{res="failure";}
           System.out.println("Login attempt "+res+" from " + getSourceInfo(socket) + " with username " + username);
        
           filename="index.html"; //TODO add a page for failed login and update username if logged in

        } 
        
        else if(method.equals("POST")&&path.equals("/payments")){
            String cardNumber = "";
            String cardName = "";
            String cardExpiry = "";
            String cardCVC = "";
            String cardZip = "";
            for (int x=0;x<parser.values.length;x++) {
                if (parser.values[x]==null){break;}
                else if (parser.values[x].equals("cardNumber")) {
                    x++;
                    cardNumber = parser.values[x];
                } else if (parser.values[x].equals("cardName")) {
                    x++;
                    cardName = parser.values[x];
                } else if (parser.values[x].equals("cardExpiry")) {
                    x++;
                    cardExpiry = parser.values[x];
                } else if (parser.values[x].equals("cardCVC")) {
                    x++;
                    cardCVC = parser.values[x];
                } else if (parser.values[x].equals("cardZip")) {
                    x++;
                    cardZip = parser.values[x];
                }
           }
                System.out.println("Payment attempt from " + getSourceInfo(socket) + " with card number " + cardNumber);
                PaymentAuthenticator authenticator = new PaymentAuthenticator(new PaymentRepository());
                String username="";
                authenticator.pay(username, cardNumber, cardName, cardExpiry, cardCVC, cardZip);
                    
                filename="finished-registration.html";
        } 
        
        else if(path.startsWith("/search")){
            String[] search=null; //stores all of the search parameters
        }
            // Map the requested path to a file
            if(filename.equals("garbage")){
                if (path.endsWith(".html")) {
                    filename = path.substring(1);    
                    mimeType = "text/html";
                } else if (path.endsWith(".css")) {
                    filename = path.substring(1); 
                    mimeType = "text/css";
                } else if (path.endsWith(".js")) {
                    filename = path.substring(1);  
                    mimeType = "application/javascript";
                } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                    filename = path.substring(1); 
                    mimeType = "image/jpeg";
                } else if (path.endsWith(".png")){
                    filename = path.substring(1);
                    mimeType = "image/png"; 
                }
                else if (path.endsWith(".ico")){
                    filename = path.substring(1);
                    mimeType = "image/x-icon";
                }
                else if (path.endsWith(".gif")){
                    filename = path.substring(1);
                    mimeType = "image/gif";
                }
                else {
                    filename="index.html";
                    mimeType = "text/html";
                }
            }
            boolean isBinary = mimeType.startsWith("image/");

            // Read the file into a byte array
           try{
             byte[] fileBytes = Files.readAllBytes(Paths.get("html/" + filename));

            // Send an HTTP response with the content
            OutputStream output = socket.getOutputStream();
            output.write(("HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes());

            if (isBinary) {
                // Write the binary data directly to the output
                output.write(fileBytes);
            } else {
                // Convert the byte array to a string and write it to the output
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(new String(fileBytes));
            }

            output.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}



