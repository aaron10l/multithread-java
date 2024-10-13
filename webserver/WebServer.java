package webserver;
/** Assignment 1
 * Aaron Alvarez
 */
import java.io.*;
import java.net.*;
import java.util.*;


public class WebServer {
  public static void main(String argv[]) throws Exception{
    int port = 6789;

    // establish listening socket
    ServerSocket serverSocket = new ServerSocket(port);
    System.out.println("Web server listening on port " + port);
    
    // process http reqs. in an infinite loop
    while (true){
      // listen for TCP connection req.
      Socket clientSocket = serverSocket.accept();

      // when a connection req. is recieved...
      HttpRequest request = new HttpRequest(clientSocket);
      Thread thread = new Thread(request);

      // start the thread
      thread.start();
    }
  }
}

final class HttpRequest implements Runnable{
  final static String CRLF = "\r\n"; // indicates EOL for headers
  Socket socket;

  // Constructor
  public HttpRequest(Socket socket) throws Exception{
    this.socket = socket;
  }

  // Implement run method of Runnable interface
  public void run(){
    try {
      processRequest();
    } catch (Exception e){
      System.out.println(e);
    }
  }

  private void processRequest() throws Exception{
    // get reference to input and output streams
    InputStream is = socket.getInputStream();
    DataOutputStream os = new DataOutputStream(socket.getOutputStream());

    // set up input stream filters 
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    // get first line of HTTP req. message
    String requestLine = br.readLine();

    // display req line
    System.out.println("Request: \n ------------------------");
    System.out.println();
    System.out.println(requestLine);

    String headerLine = null;
    while ((headerLine = br.readLine()).length() != 0){
      System.out.println(headerLine);
    }

    // extract filename from request line
    StringTokenizer tokens = new StringTokenizer(requestLine);
    tokens.nextToken();
    String fileName = tokens.nextToken();

    // prepend a "." so that the filename req. is in current directory
    fileName = "." + fileName;

    // open the requested file.
    FileInputStream fis = null;
    boolean fileExists = true;
    try {
      fis = new FileInputStream(fileName);
    } catch (FileNotFoundException e){
      fileExists = false;
    }

    // constructing the response message
    String statusLine = null;
    String contentTypeLine = null;
    String entityBody = null;
    if (fileExists){
      statusLine = "HTTP/1.1 200 OK" + CRLF;
      contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
    } else { //if requested file is not a .txt, report error to web client
      if (!contentType(fileName).equalsIgnoreCase("text/plain")){
        statusLine = "HTTP/1.1 404 Not Found" + CRLF;
        contentTypeLine = "Content-type: text/html" + CRLF;
        entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                   "<BODY>Not Found</BODY></HTML>";
      } else { // retrieve txt file from ftp server
        statusLine = "HTTP/1.1 200 OK" + CRLF;
        contentTypeLine = "Content-type: text/plain" + CRLF;

        // create instance of ftp client
        FtpClient ftpClient = new FtpClient();

        // connect to ftp server
        ftpClient.connect("hello", "world");

        // retrieve file from ftp server
        ftpClient.getFile(fileName);
        //System.out.println(fileName);

        // disconnect from ftp server
        ftpClient.disconnect();

        // assign input strema to read recently downloaded ftp file
        fis = new FileInputStream(fileName);
      }
    }

    // printing response
    System.out.println("\nResponse: \n ------------------------");
    System.out.println(statusLine);
    System.out.println(contentTypeLine);
    if (!fileExists){
      System.out.println(entityBody);
    }

    // send the status line
    os.writeBytes(statusLine);

    // send the content type line
    os.writeBytes(contentTypeLine);
    os.writeBytes(CRLF);

    // sending entity body.. if exists
    if (fileExists){
      sendBytes(fis, os);
      fis.close();
    } else {
      if (!contentType(fileName).equalsIgnoreCase("text/plain")){
        os.writeBytes(entityBody);
      } else {
        sendBytes(fis, os);
        fis.close();
      }
    }

    // close streams and socket
    os.close();
    br.close();
    socket.close();
  }

  private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception{
    // construct a 1k buffer to hold bytes otw to socket
    byte[] buffer = new byte[1024];
    int bytes = 0;

    //copy requested file into the socket's output stream
    while ((bytes = fis.read(buffer)) != -1){
      os.write(buffer, 0, bytes);
    }
  }

  private static String contentType(String fileName){
    if (fileName.endsWith(".htm") || fileName.endsWith(".html")){
      return "text/html";
    } else if (fileName.endsWith(".gif")){
      return "image/gif";
    } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")){
      return "image/jpeg";
    } else if (fileName.endsWith(".png")){
      return "image/png";
    } else if (fileName.endsWith(".txt")){
      return "text/plain";
    } else {
      return "application/octet-stream";
    }
  }
}


