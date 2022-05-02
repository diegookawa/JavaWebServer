import java.io.* ;
import java.net.* ;
import java.util.* ;

public final class WebServer {

    public static void main (String argv[]) throws Exception {

        int port = 6789;
        
        // Establish the listen socket.
        ServerSocket serverSocket = new ServerSocket(port);
        System.err.println("Server is running on port: " + port);

        while (true) {

            // Listen for a TCP connection request.
            Socket socket = serverSocket.accept();
            System.err.println("Client connected");

            // Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(socket);
            
            // Create a new thread to process the request.
            Thread thread = new Thread(request);

            // Start the thread.
            thread.start();

        }

    }

}

final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";
    Socket socket;

    //Constructor
    public HttpRequest (Socket socket) throws Exception {

        this.socket = socket;

    }

    // Implement the run() method of the Runnable interface.
    public void run() {

        try {
            
            processRequest();

        } 
        
        catch (Exception e) {

            System.out.println(e);

        }

    }

    private void processRequest() throws Exception {

        // Get a reference to the socket's input and output streams.
        InputStream is = new DataInputStream(socket.getInputStream());
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Set up input stream filters.
        InputStreamReader isr = new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr);

        // Get the request line of the HTTP request message.
        String requestLine = br.readLine();

        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken(); // skip over the method, which should be "GET"
        String fileName = tokens.nextToken();

        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;

        // Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;

        try {
            
            fis = new FileInputStream(fileName);
            
        } 
        
        catch (FileNotFoundException e) {

            fileExists = false;

        }

        // Construct the response message.
        String contentTypeLine = null;
        String entityBody = null;

        if(fileExists) {

            System.err.println("File exists");
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            // Send the content type line.
            os.write((contentTypeLine).getBytes());
            // Send a blank line to indicate the end of the header lines.
            os.write(CRLF.getBytes()); 

            // Send the entity body.
            sendBytes(fis, os);

        }

        else {

            System.err.println("File not found");
            contentTypeLine = "File not found";
            entityBody = "<html>" + "<head><title>NOT FOUND</title><head>" + "<body>NOT FOUND</body></html>";

            os.write("HTTP/1.1 404 Not Found\r\n".getBytes());
            // Send the content type line.
            os.write(("ContentType: text/html\r\n").getBytes());
            // Send a blank line to indicate the end of the header lines.
            os.write(CRLF.getBytes()); 

            os.write(contentTypeLine.getBytes());

        }

        // Display the request line.
        System.out.println();
        System.out.println(requestLine);
        
        // Get and display the header lines.
        String headerLine = null;

        while ((headerLine = br.readLine()).length() != 0) 
            System.out.println(headerLine);

        os.close();
        br.close();
        socket.close();

    }

    private String contentType(String fileName) {

        if(fileName.endsWith(".htm") || fileName.endsWith(".html"))
            return "text/html";                      

        else if(fileName.endsWith(".gif"))
            return "image/gif";

        else if(fileName.endsWith(".jpeg"))
            return "image/jpeg";
        
        else
            return "application/octet-stream";

    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {

        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1) 
            os.write(buffer, 0, bytes);

    }

}