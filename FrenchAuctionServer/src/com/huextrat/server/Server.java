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
package com.huextrat.server;

import com.huextrat.item.Item;
import com.huextrat.messages.Message;
import com.huextrat.messages.MessageType;
import com.huextrat.messages.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class Server {

    private ServerSocket listener;
    
    
    private static ServerMainController con;
    
    private static final HashMap<String, User> names = new HashMap<>();
    private static final HashSet<ObjectOutputStream> writers = new HashSet<>();
    private static final ArrayList<User> users = new ArrayList<>();

    public HashMap<String, User> getNames() {
        return names;
    }
    
    public ServerSocket getListener(){
        return listener;
    }

    public HashSet<ObjectOutputStream> getWriters() {
        return writers;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    Server(String address, int port, ServerMainController con) throws IOException{
        this.con = con;

        listener = new ServerSocket(port);
        updateUI(new Message("INFO", MessageType.SERVER, "Server is started"), Color.GREEN);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (IOException e) {
        } finally {
            listener.close();
        }
        
    }
    

    public static class Handler extends Thread {
        private String name;
        private final Socket socket;
        private User user;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;
        
        private static int startBid = 0;
        public static NavigableMap<Integer, String> itemBid = new TreeMap<>();
        
        public static boolean isCurrentAuction = false;
        public static Item currentItem;


        public Handler(Socket socket) throws IOException {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                is = socket.getInputStream();
                input = new ObjectInputStream(is);
                os = socket.getOutputStream();
                output = new ObjectOutputStream(os);

                Message firstMessage = (Message) input.readObject();
                
                Message messageIsTrying = new Message(firstMessage.getName(), MessageType.USER, " is trying to connect. "+socket.getInetAddress());
                updateUI(messageIsTrying, Color.GREEN);
            
                
                checkDuplicateUsername(firstMessage);
                writers.add(output);
                addToList(firstMessage);
                
                if(isCurrentAuction){
                    output.writeObject((new Message("SEVER", MessageType.NEWITEM, currentItem)));
                    output.reset();
                }
                
                while (socket.isConnected()) {
                    Message inputmsg = (Message) input.readObject();
                    if (inputmsg != null) {
                        switch (inputmsg.getType()) {
                            case USER:
                                Message messageUser = new Message(inputmsg.getName(), MessageType.USER, inputmsg.getMsg());
                                updateUI(messageUser, Color.BLUE);
                                write(inputmsg);
                                break;
                            case CONNECTED:
                                addToList(inputmsg);
                                
                                break;
                            case NEWBID:
                                
                                if(itemBid.isEmpty()){
                                    if(Integer.parseInt(inputmsg.getMsg()) > startBid){
                                        itemBid.put(Integer.parseInt(inputmsg.getMsg()), inputmsg.getName());
                                        write(new Message("SERVER", MessageType.SERVER, inputmsg.getName()+" has bidded at "+inputmsg.getMsg()));
                                        Message newHighestBid = new Message("SERVER", MessageType.NEWHIGHESTBID, Integer.parseInt(inputmsg.getMsg())+"");
                                        write(newHighestBid);
                                    }
                                    else {
                                        output.writeObject(new Message("SERVER", MessageType.SERVER, "Bid too low!"));
                                        output.reset();
                                    }
                                }
                                else {
                                    if(itemBid.lastEntry().getKey() < Integer.parseInt(inputmsg.getMsg())){
                                        itemBid.put(Integer.parseInt(inputmsg.getMsg()), inputmsg.getName());
                                        write(new Message("SERVER", MessageType.SERVER, inputmsg.getName()+" has bidded at "+inputmsg.getMsg()));
                                        Message newHighestBid = new Message("SERVER", MessageType.NEWHIGHESTBID, Integer.parseInt(inputmsg.getMsg())+"");
                                        write(newHighestBid);
                                    }
                                    else {
                                        output.writeObject(new Message("SERVER", MessageType.SERVER, "Bid too low!"));
                                        output.reset();
                                    }
                                }
                                
                                break; 
                        }
                    }
                }
            } catch (SocketException socketException) {
                System.out.println(socketException);
            }catch (Exception e){
            } finally {
                closeConnections();
            }
        }

        private synchronized void checkDuplicateUsername(Message firstMessage) throws Exception {
            //logger.info(firstMessage.getName() + " is trying to connect");
            
            if (!names.containsKey(firstMessage.getName())) {
                this.name = firstMessage.getName();
                user = new User();
                user.setName(firstMessage.getName());
                user.setIp(socket.getInetAddress().toString());

                users.add(user);
                names.put(name, user);

                //logger.info(name + " has been added to the list");
                Message messageConnected = new Message(name, MessageType.USER, "is connected: "+user.getIp());
                updateUI(messageConnected, Color.GREEN);
            } else {
                //logger.error(firstMessage.getName() + " is already connected");
                Message messageConnected = new Message(firstMessage.getName(), MessageType.USER, "is already connected.");
                updateUI(messageConnected, Color.RED);
                throw new Exception(firstMessage.getName() + " is already connected");
            }
        }

        private static Message removeFromList() throws IOException {
            
            updateUsersListUI();
            
            //logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(names);
            write(msg);
            //logger.debug("removeFromList() method Exit");
            return msg;
        }

        public static Message addToList(Message msg) throws IOException {
            
            updateUsersListUI();
            
            msg.setMsg(msg.getName()+" joined the server!");
            msg.setType(MessageType.CONNECTED);
            msg.setName("SERVER");
            write(msg);
            return msg;
        }
        
        public static Message addNewUser() throws IOException{
            
            updateUsersListUI();
            
            Message msg = new Message();
            msg.setMsg("New user joined the server!");
            msg.setType(MessageType.SERVER);
            msg.setName("SERVER");
            write(msg);
            return msg;
        }
        
        public static void sendWinner() throws IOException{
            if(!itemBid.isEmpty()){
                Entry<Integer, String> lastEntry = itemBid.lastEntry();


                Message messageAuctionClosed = new Message("SERVER", MessageType.SERVER, "This auction is now closed!");
                updateUI(messageAuctionClosed, Color.ORANGE);
                
                Message messageWinner = new Message("SERVER", MessageType.SERVER, lastEntry.getValue()+" wins this auction with a "+lastEntry.getKey()+" bid");
                updateUI(messageWinner, Color.ORANGE);
                for (ObjectOutputStream writer : writers) {
                    writer.writeObject(messageAuctionClosed);
                    writer.writeObject(messageWinner);
                    writer.reset();
                }
            }
            else {
                write(new Message("SERVER", MessageType.SERVER, "This auction is now closed!"));
                write(new Message("SERVER", MessageType.SERVER, "No one wins this auction!"));
            }
        }

        public static void write(Message msg) throws IOException {
            if(msg.getType().equals(MessageType.NEWITEM)){
                //itemBid.put(msg.getItem().getHighestBid(), "SERVER");
                isCurrentAuction = true;
                currentItem = msg.getItem();
                startBid = msg.getItem().getHighestBid();
            }
            if(msg.getType().equals(MessageType.SERVER)){
                Message messageIsTrying = new Message(msg.getName(), MessageType.USER, msg.getMsg());
                updateUI(messageIsTrying, Color.ORANGE);
            }
            for (ObjectOutputStream writer : writers) {
                msg.setUserlist(names);
                msg.setUsers(users);
                msg.setOnlineCount(names.size());
                writer.writeObject(msg);
                writer.reset();
            }
        }
        
        public synchronized void closeConnections()  {
            if (name != null) {
                names.remove(name);
                Message messageDisconnected = new Message(name, MessageType.USER, "is disconnected.");
                updateUI(messageDisconnected, Color.ORANGE);
            }
            if (user != null){
                users.remove(user);
            }
            if (output != null){
                writers.remove(output);
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (input != null){
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            try {
                removeFromList();
            } catch (IOException e) {
            }
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private static void updateUsersListUI(){
        Platform.runLater(() -> {
            ObservableList<String> listUserName = FXCollections.observableArrayList();
            
            users.forEach((u) -> {
                listUserName.add(u.getName());
            });
            
            con.userList.setItems(listUserName);
            con.onlineCountLabel.setText(String.valueOf(listUserName.size()));
        });
    }
    
    public static void updateUI(Message msg, Color color){
        Platform.runLater(
            () -> {
                CornerRadii corn = new CornerRadii(10);
                Insets ins = new Insets(5);
                
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();

                Label lab = new Label(dateFormat.format(date)+" - "+msg.getName()+": "+msg.getMsg());
                lab.setWrapText(true);
                lab.setBackground(new Background(new BackgroundFill(color, corn, null)));
                lab.setTextFill(Color.WHITE);
                lab.setFont(new Font("Arial", 14));
                lab.setPadding(ins);

                HBox hBox=new HBox();
                hBox.getChildren().add(lab);
                hBox.setAlignment(Pos.CENTER);
                Separator separator1 = new Separator();

                con.chatPane.getChildren().addAll(hBox, separator1);
                con.chatPane.setPadding(ins);
                con.chatPane.setSpacing(10);
            }
        );

        Platform.runLater( () -> con.scrollPane.vvalueProperty().bind(con.chatPane.heightProperty()));
    }
    
}
