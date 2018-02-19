/* 
 * The MIT License
 *
 * Copyright 2018 huextrat <extrat.h@gmail.com> <www.hugoextrat.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.huextrat.client;

import com.huextrat.login.LoginController;
import com.huextrat.messages.Message;
import com.huextrat.messages.MessageType;

import java.io.*;
import java.net.Socket;

import static com.huextrat.messages.MessageType.CONNECTED;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * 
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class Listener implements Runnable{

    private final ClientMainController controller;
    private final LoginController logController;
    
    private final String hostname;
    private final int port;
    private final String username;
    
    private Socket socket;

    private ObjectOutputStream oos;
    private InputStream is;
    private ObjectInputStream input;
    private OutputStream outputStream;

    /**
     * Set listener for client
     * @param hostname
     * @param port
     * @param username
     * @param controller
     * @param logController 
     */
    public Listener(String hostname, int port, String username, ClientMainController controller, LoginController logController) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.controller = controller;
        this.logController = logController;
    }
    
    /**
     * Return the socket
     * @return 
     */
    public Socket getSocket(){
        return socket;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(hostname, port);
            logController.showScene();
            outputStream = socket.getOutputStream();
            oos = new ObjectOutputStream(outputStream);
            is = socket.getInputStream();
            input = new ObjectInputStream(is);
        } catch (IOException e) {
            logController.showErrorDialog("Could not connect to server");
        }

        try {
            connect();
            
            controller.addAsServer(new Message("admin", MessageType.SERVER, "Welcome, you have joined the server!"));

            if(socket!=null){
                while (socket.isConnected()) {
                    Message message = null;
                    try{
                        message = (Message) input.readObject();
                    }
                    catch (EOFException e){
                        duplicateUsername();
                        break;
                    }


                    if (message != null) {
                        System.out.println(message.getType());
                        switch (message.getType()) {
                            case USER:
                                controller.addToChat(message);
                                break;
                            case SERVER:
                                controller.addAsServer(message);
                                break;
                            case CONNECTED:
                                controller.setUserList(message);
                                controller.addAsServer(message);
                                break;
                            case DISCONNECTED:
                                controller.addAsServer(new Message("admin", MessageType.SERVER, "An user has logged out!"));
                                controller.setUserList(message);
                                break;
                            case NEWITEM:
                                controller.addToItem(message);
                                break;
                            case NEWHIGHESTBID:
                                controller.newHighestBid(message);
                                break;
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            controller.logoutScene();
        }
    }
    
    /**
     * Send to server new bid message
     * @param i
     * @throws IOException 
     */
    public void newBid(int i) throws IOException{
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.NEWBID);
        createMessage.setMsg(i+"");
        oos.writeObject(createMessage);
        oos.flush();
    }

    /**
     * Send to server a message
     * @param msg
     * @throws IOException 
     */
    public void send(String msg) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.USER);
        createMessage.setMsg(msg);
        oos.writeObject(createMessage);
        oos.flush();
    }

    /**
     * Send to server new client is connected
     * @throws IOException 
     */
    public void connect() throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(CONNECTED);
        createMessage.setMsg("has connected");
        if(oos!=null){
            oos.writeObject(createMessage);
        }
    }
    
    /**
     * Called if client username is already used
     */
    public void duplicateUsername() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "This username is not available!");
            alert.setHeaderText("Username error!");
            alert.showAndWait();
            Platform.exit();
            System.exit(0);
        });
    }

}
