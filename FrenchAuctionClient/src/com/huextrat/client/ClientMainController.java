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

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import com.huextrat.messages.Message;
import com.huextrat.messages.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * 
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class ClientMainController implements Initializable {

    @FXML private TextField messageBox;
    @FXML private Label usernameLabel;
    @FXML private Label onlineCountLabel;
    @FXML private JFXListView userList;
    @FXML private Text time;
    @FXML private Text nameText, descriptionText, endTimeText, remainingTimeText;
    @FXML private VBox chatPane;
    @FXML private JFXTextField makeBidTextField;
    @FXML private JFXButton makeBidButton, logoutButton;
    @FXML private Text highestBidText;
    @FXML private ScrollPane scrollPane;
    
    private int minute;
    private int hour;
    private int second;
    
    private Listener listener;
    private Timeline timeline = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        logoutButton.setOnAction((ActionEvent event) -> {
            logoutButtonPressed();
        });
        
        makeBidTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                makeBidTextField.setText(newValue.replaceAll("[\\D]",""));
            }
        });
        
        Timeline clock;
        clock = new Timeline(new KeyFrame(javafx.util.Duration.ONE, e -> {    
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ke.consume();
            }
        });
        
        if(timeline == null){
            disableMakeABid();
        }
    }
    
    public void setListener(Listener listener){
        this.listener = listener;
    }
    
    public void disableMakeABid(){
        makeBidButton.setDisable(true);
        makeBidTextField.setDisable(true);
        highestBidText.setText("0");
    }
    
    public void enableMakeABid(){
        makeBidButton.setDisable(false);
        makeBidTextField.setDisable(false);
    }
    
    
    public void sendButtonAction() throws IOException {
        String msg = messageBox.getText();
        if (!messageBox.getText().isEmpty()) {
            listener.send(msg);
            messageBox.clear();
        }
    }
    
    @FXML
    public void makeNewBidAction(ActionEvent event) throws IOException{
        int bid = Integer.parseInt(makeBidTextField.getText());
        listener.newBid(bid);
        makeBidTextField.clear();
    }
    
    public synchronized void newHighestBid(Message msg){
        highestBidText.setText(msg.getMsg());
        int highestbid = Integer.parseInt(msg.getMsg())+1;
        makeBidTextField.setText(highestbid+"");
    }

    public synchronized void addToItem(Message msg){
        
        if(timeline != null){
            timeline.stop();
        }
        
        
        nameText.setText("");
        descriptionText.setText("");
        endTimeText.setText("");
        remainingTimeText.setText("");
        
        nameText.setText(msg.getItem().getName());
        descriptionText.setText(msg.getItem().getDescription());
        
        int i = msg.getItem().getEndTime();
        int hours = i /3600;
        int minutes = (i % 3600)/60;
        int seconds = i % 60;
        String endTime = hours+":"+minutes+":"+seconds;
        endTimeText.setText(endTime);
        
        highestBidText.setText(msg.getItem().getHighestBid()+"");
        int highestbid = msg.getItem().getHighestBid()+1;
        makeBidTextField.setText(highestbid+"");

        timeline = new Timeline(
            new KeyFrame(
                javafx.util.Duration.ONE,
                event -> {
                    ZonedDateTime now = ZonedDateTime.now();
                    ZonedDateTime midnight = now.truncatedTo(ChronoUnit.DAYS);
                    Duration duration = Duration.between(midnight, now);
                    long secondsPassed = duration.getSeconds();
                    final long diff = msg.getItem().getEndTime() - secondsPassed;
                    if ( diff < 0 ) {
                        remainingTimeText.setText(0+" seconds");
                        makeBidTextField.setText("");
                        disableMakeABid();
                    } else {
                        remainingTimeText.setText(diff+" seconds");
                    }
                }
            )
        );
        timeline.setCycleCount( Animation.INDEFINITE );
        timeline.play();
        
        enableMakeABid();
    }
    
    public synchronized void addToChat(Message msg) {

        if (msg.getName().equals(usernameLabel.getText())) {
            Platform.runLater(
                () -> {
                    CornerRadii corn = new CornerRadii(10);
                    Insets ins = new Insets(5);
                    
                    Label lab = new Label(msg.getMsg()+" :"+msg.getName());
                    lab.setMaxWidth(500);
                    lab.setBackground(new Background(new BackgroundFill(Color.GREEN, corn, null)));
                    lab.setTextFill(Color.WHITE);
                    lab.setWrapText(true);
                    lab.setFont(new Font("Arial", 13));
                    lab.setPadding(ins);
                    
                    HBox hBox=new HBox();
                    hBox.getChildren().add(lab);
                    hBox.setAlignment(Pos.CENTER_RIGHT);
                    Separator separator1 = new Separator();
                    
                    chatPane.getChildren().addAll(hBox, separator1);
                    chatPane.setPadding(ins);
                    chatPane.setSpacing(10);
                }
            );

        } else {
            Platform.runLater(
                () -> {
                    CornerRadii corn = new CornerRadii(10);
                    Insets ins = new Insets(5);
                    
                    Label lab = new Label(msg.getName()+": "+msg.getMsg());
                    lab.setMaxWidth(500);
                    lab.setBackground(new Background(new BackgroundFill(Color.BLUE, corn, null)));
                    lab.setTextFill(Color.WHITE);
                    lab.setFont(new Font("Arial", 13));
                    lab.setPadding(ins);
                    lab.setWrapText(true);
                    
                    HBox hBox=new HBox();
                    hBox.getChildren().add(lab);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    Separator separator1 = new Separator();
                    
                    chatPane.getChildren().addAll(hBox, separator1);
                    chatPane.setPadding(ins);
                    chatPane.setSpacing(10);
                }
            );
        }
        
        Platform.runLater( () -> scrollPane.vvalueProperty().bind(chatPane.heightProperty()));
    }
    public void setUsernameLabel(String username) {
        this.usernameLabel.setText(username);
    }

    public void setUserList(Message msg) {
        Platform.runLater(() -> {
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            ObservableList<String> listUserName = FXCollections.observableArrayList();
            users.forEach((u) -> {
                listUserName.add(u.getName());
            });
            
            userList.setItems(listUserName);
            onlineCountLabel.setText(String.valueOf(listUserName.size()));
        });
    }


    public void sendMethod(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            sendButtonAction();
        }
    }

    public synchronized void addAsServer(Message msg) {
        
        Platform.runLater(
                () -> {
                    CornerRadii corn = new CornerRadii(10);
                    Insets ins = new Insets(5);
                    
                    Label lab = new Label(msg.getMsg());
                    lab.setWrapText(true);
                    lab.setBackground(new Background(new BackgroundFill(Color.ORANGE, corn, null)));
                    lab.setTextFill(Color.WHITE);
                    lab.setFont(new Font("Arial", 14));
                    lab.setPadding(ins);
                    
                    HBox hBox=new HBox();
                    hBox.getChildren().add(lab);
                    hBox.setAlignment(Pos.CENTER);
                    Separator separator1 = new Separator();
                    
                    chatPane.getChildren().addAll(hBox, separator1);
                    chatPane.setPadding(ins);
                    chatPane.setSpacing(10);
                }
            );
        
        Platform.runLater( () -> scrollPane.vvalueProperty().bind(chatPane.heightProperty()));
    }
    
    public void logoutButtonPressed(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setContentText("Are you sure you want to logout?");
        ButtonType okButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(okButton, noButton);
        alert.showAndWait().ifPresent(type -> {
            
                if (type.getButtonData().equals(okButton.getButtonData())) {
                    Platform.runLater(() -> {
                        Alert alertDc = new Alert(Alert.AlertType.INFORMATION, "You are disconnected!");
                        alertDc.setHeaderText("Log out!");
                        alertDc.showAndWait();
                        Platform.exit();
                        System.exit(0);
                    });
                } else if (type.getButtonData().equals(okButton.getButtonData())) {
                    
                } else {
                }
        });
    }

    public void logoutScene() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The server has been shut down!");
            alert.setHeaderText("Unknown server!");
            alert.showAndWait();
            Platform.exit();
            System.exit(0);
        });
    }
    
    @FXML
    public void closeApplication() {
        Platform.exit();
        System.exit(0);
    }
}