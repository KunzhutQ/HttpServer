package com.kunzhut.httpserver;

import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServer {
    public static final int DefaultPort = 7500;
    public static final int DefaultSocketTimeOut=5000;
    private ServerSocket HttpSocket;
    private int port = DefaultPort;
    private volatile int socketTimeout=DefaultSocketTimeOut;
    private volatile Config config;
    private Thread RequestHandler;

    public HttpServer(Config config) {
        this.config= config;
    }
    public HttpServer(int port, Config config) {
        this.port=port;
        this.config= config;
    }
    public HttpServer(int port, int socketTimeout, Config config){
        this.port=port;
        this.socketTimeout=socketTimeout;
        this.config= config;
    }
    public HttpServer(Config config, int socketTimeout){
        this.socketTimeout=socketTimeout;
        this.config= config;
    }

    public int getPort() {
        return port;
    }

    public synchronized void setPort(int port) {
        this.port = port;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public synchronized void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public synchronized void setConfig(Config config) {
        this.config = config;
    }

    public synchronized void start() throws IOException {
        if(HttpSocket==null) {
            HttpSocket = new ServerSocket(port);
            RequestHandler = getNewHandlerInstance();
            RequestHandler.start();
        }else {
            throw new IOException("Server already started");
        }

    }



    public synchronized void stop() throws IOException {
        if(HttpSocket!=null) {
            HttpSocket.close();
            if (HttpSocket.isClosed()) HttpSocket = null;
        }else {
            throw new IOException("Server not started");
        }

    }

    public int status(){
        return HttpSocket!=null ? 1 : 0;
    }

    private Thread getNewHandlerInstance(){
        return new Thread(()->{
            try {
              while (HttpSocket!=null){
                    Socket socket = HttpSocket.accept();
                    socket.setSoTimeout(socketTimeout);
                    new Thread(()->{
                        try {
                            byte[] responseBody = Files.readAllBytes(Paths.get(config.getRoot() + getPathFromRequest(getRequest(socket))));
                            Config.Header header = config.getHeader();
                            socket.getOutputStream().write(createResponse(header,responseBody));

                        } catch (IOException ignored) {

                        } finally {
                            try {
                                socket.close();
                            } catch (IOException ignore) {}
                        }
                    }).start();
              }

            } catch (IOException ignored) {}
        });
    }

    private static String getPathFromRequest(byte[] bytes){
       Matcher m = PathFromRequest.matcher(new String(bytes));
       return m.find() ? m.group(1) : null;
    }
    private static final Pattern PathFromRequest = Pattern.compile("(?<=GET\\s)(/.*)(?=\\sHTTP/\\d\\.\\d)", Pattern.MULTILINE | Pattern.DOTALL);

    private static byte[] concatenate(byte[] arr1, byte[] arr2){
        byte[] arr3 = new byte[arr1.length+arr2.length];
        System.arraycopy(arr1,0, arr3, 0, arr1.length);
        System.arraycopy(arr2,0, arr3, arr1.length, arr3.length-arr1.length);
        return arr3;
    }

    private static byte[] getRequest(Socket socket) throws IOException {
        byte[] buf = new byte[4096];
        byte[] bytes = new byte[socket.getInputStream().read(buf)];
        System.arraycopy(buf,0, bytes,0,bytes.length);
        return bytes;
    }
    private static byte[] createResponse(Config.Header header, byte[] body){
         header.addBlankLine();
         return concatenate(header.toString().getBytes(), body);
    }

}
