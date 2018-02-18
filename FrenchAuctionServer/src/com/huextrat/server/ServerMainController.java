/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.huextrat.server;

import com.huextrat.item.Item;
import com.huextrat.messages.Message;
import com.huextrat.messages.MessageType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTimePicker;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 *
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class ServerMainController implements Initializable {

    @FXML private Text time;
    @FXML private JFXTextField itemName, itemDescription, startBidTextField;
    @FXML private JFXTimePicker endTimePicker;
    @FXML private TextField messageBox;
    @FXML private JFXButton buttonSend;
    
    @FXML private Text nameText, descriptionText, endTimeText, remainingTimeText;
    //highestBidText;
    
    @FXML Label onlineCountLabel;
    @FXML JFXListView userList;
    
    @FXML ScrollPane scrollPane;
    @FXML VBox chatPane;
    
    private Server server;
    private String serverAddress;
    private int serverPort;
    
    private int minute;
    private int hour;
    private int second;
    
    private ServerMainController con;

    private Timeline timeline = null;
    
    public void setController(ServerMainController con){
        this.con = con;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        endTimePicker.setValue(LocalTime.now());
        
        startBidTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                startBidTextField.setText(newValue.replaceAll("[\\D]",""));
            }
        });
        
        Timeline clock;
        clock = new Timeline(new KeyFrame(Duration.ONE, e -> {    
            Calendar cal = Calendar.getInstance();
            second = cal.get(Calendar.SECOND);
            minute = cal.get(Calendar.MINUTE);
            hour = cal.get(Calendar.HOUR_OF_DAY);
            time.setText(hour + ":" + minute + ":" + second);
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendButtonAction();
                    ke.consume();
                } catch (IOException ex) {
                    //ex.printStackTrace();
                }
                
            }
        });
       
    }
    
    void setServerAdress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    void startServer() throws IOException {
        new Thread() {
            @Override
            public void run() {
                try {
                    server = new Server(serverAddress, serverPort, con);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }.start();
    }
    
    @FXML
    public void startNewAuction(ActionEvent event) throws IOException{
        
        if(itemName.getText().isEmpty()){
            itemName.setPromptText("A valid name is required!");
            itemName.setFocusColor(Color.RED);
            itemName.requestFocus();
        }
        else if(itemDescription.getText().isEmpty()){
            itemDescription.setPromptText("A valid description is required!");
            itemDescription.setFocusColor(Color.RED);
            itemDescription.requestFocus();
        }
        else if(startBidTextField.getText().isEmpty()){
            startBidTextField.setPromptText("A firt minimal bid is required!");
            startBidTextField.setFocusColor(Color.RED);
            startBidTextField.requestFocus();
        }
        else {
            itemName.setPromptText("Item name:");
            itemName.setFocusColor(Color.rgb(77,77,77));
            itemDescription.setPromptText("Item description:");
            itemDescription.setFocusColor(Color.rgb(77,77,77));
            startBidTextField.setPromptText("Min bid:");
            startBidTextField.setFocusColor(Color.rgb(77,77,77));
            
            if(timeline != null){
                timeline.stop();
            }
            Item item = new Item(itemName.getText(), itemDescription.getText(), endTimePicker.getValue().toSecondOfDay(), Integer.parseInt(startBidTextField.getText()));

            Message msg = new Message();
            msg.setMsg("New item is available: "+ itemName.getText());
            msg.setType(MessageType.SERVER);
            msg.setName("SERVER");

            Server.Handler.write(msg);

            Message msgItem = new Message("SERVER", MessageType.NEWITEM, item);
            Server.Handler.write(msgItem);

            itemName.setText("");
            itemDescription.setText("");
            endTimePicker.setValue(LocalTime.now());
            startBidTextField.setText("");

            buttonSend.setDisable(false);

            nameText.setText(msgItem.getItem().getName());
            descriptionText.setText(msgItem.getItem().getDescription());

            int i = msgItem.getItem().getEndTime();
            int hours = i /3600;
            int minutes = (i % 3600)/60;
            int seconds = i % 60;
            String endTime = hours+":"+minutes+":"+seconds;
            endTimeText.setText(endTime);

            //highestBidText.setText(msgItem.getItem().getHighestBid()+"");

            timeline = new Timeline(
                new KeyFrame(
                    javafx.util.Duration.millis(1),
                    eventTimeline -> {
                        ZonedDateTime now = ZonedDateTime.now();
                        ZonedDateTime midnight = now.truncatedTo(ChronoUnit.DAYS);
                        java.time.Duration duration = java.time.Duration.between(midnight, now);
                        long secondsPassed = duration.getSeconds();
                        final long diff = msgItem.getItem().getEndTime() - secondsPassed;
                        if ( diff < 0 ) {
                            remainingTimeText.setText(0+" seconds");
                            try {
                                if(timeline.getStatus() == Status.RUNNING){
                                    Server.Handler.sendWinner();
                                    timeline.stop();
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(ServerMainController.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } else {
                            remainingTimeText.setText(diff+" seconds");
                        }
                    }
                )
            );
            timeline.setCycleCount( Animation.INDEFINITE );
            timeline.play();

            Server.Handler.itemBid.clear();
        }
    }
    
    @FXML 
    public void sendButtonAction() throws IOException{
        String msg = messageBox.getText();
        Message message = new Message("SERVER", MessageType.SERVER, msg);
        if (!messageBox.getText().isEmpty()) {
            Server.Handler.write(message);
            messageBox.clear();
        }
    }
    
    @FXML 
    public void sendMethod(KeyEvent event) throws IOException{
        if (event.getCode() == KeyCode.ENTER) {
            sendButtonAction();
        }
    }
    
    @FXML
    public void closeServerButton(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setContentText("Are you sure you want to close the server?");
        ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(okButton, noButton);
        alert.showAndWait().ifPresent(type -> {
            
                if (type.getButtonData().equals(okButton.getButtonData())) {
                   
                    Platform.exit();
                    System.exit(0);
                } else if (type.getButtonData().equals(okButton.getButtonData())) {
                    
                } else {
                }
        });
    }
}
