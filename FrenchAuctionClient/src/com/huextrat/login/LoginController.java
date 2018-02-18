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
package com.huextrat.login;

import com.huextrat.chat.ChatController;
import com.huextrat.chat.Listener;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

/**
 * 
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class LoginController implements Initializable {

    @FXML public  JFXTextField hostnameTextField;
    @FXML private JFXTextField portTextField;
    @FXML private JFXTextField usernameTextField;
    public static ChatController con;
    private Scene scene;

    private static LoginController instance;

    public LoginController() {
        instance = this;
    }

    public static LoginController getInstance() {
        return instance;
    }
    public void loginButtonAction() throws IOException {
        
        if(usernameTextField.getText().isEmpty()){
            usernameTextField.setPromptText("A valid username is required!");
            usernameTextField.setFocusColor(Color.RED);
            usernameTextField.requestFocus();
        }
        else if(hostnameTextField.getText().isEmpty()){
            hostnameTextField.setPromptText("A valid hostname is required!");
            hostnameTextField.setFocusColor(Color.RED);
            hostnameTextField.requestFocus();
        }
        else if(portTextField.getText().isEmpty()){
            portTextField.setPromptText("A valid port is required!");
            portTextField.setFocusColor(Color.RED);
            portTextField.requestFocus();
        }
        else {
            usernameTextField.setFocusColor(Color.BLACK);
            usernameTextField.setUnFocusColor(Color.BLACK);
            portTextField.setFocusColor(Color.BLACK);
            portTextField.setUnFocusColor(Color.BLACK);
            hostnameTextField.setFocusColor(Color.BLACK);
            hostnameTextField.setUnFocusColor(Color.BLACK);
            
            String hostname = hostnameTextField.getText();
            int port = Integer.parseInt(portTextField.getText());
            String username = usernameTextField.getText();

            FXMLLoader fmxlLoader = new FXMLLoader(getClass().getClassLoader().getResource("com/huextrat/views/ClientMain.fxml"));
            Parent window = (Pane) fmxlLoader.load();
            con = fmxlLoader.<ChatController>getController();

            Listener listener = new Listener(hostname, port, username, con);
            con.setListener(listener);
            Thread x = new Thread(listener);
            x.start();
            this.scene = new Scene(window);
        }
    }

    public void showScene() throws IOException {
        Platform.runLater(() -> {
            Stage stage = (Stage) hostnameTextField.getScene().getWindow();
            stage.setResizable(false);

            stage.setOnCloseRequest((WindowEvent e) -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(this.scene);
            stage.setTitle("FrenchAuction - Client");
            stage.centerOnScreen();
            con.setUsernameLabel(usernameTextField.getText());
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        portTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                portTextField.setText(newValue.replaceAll("[\\D]",""));
            }
        });
    }
    
    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }

    public void minimizeWindow(){
        MainLauncher.getPrimaryStage().setIconified(true);
    }

    public void showErrorDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText(message);
            alert.setContentText("Please check for firewall issues and check if the server is running.");
            alert.showAndWait();
        });

    }
}
