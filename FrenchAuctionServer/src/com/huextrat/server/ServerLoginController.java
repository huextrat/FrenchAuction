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

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 *
 * @author huextrat <extrat.h@gmail.com> <www.hugoextrat.com>
 */
public class ServerLoginController implements Initializable {
    
    // UI ELEMENTS
    @FXML private JFXTextField txtServerAddress;
    @FXML private JFXTextField txtPort;
    @FXML private JFXButton buttonCreateServer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        txtPort.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtPort.setText(newValue.replaceAll("[\\D]",""));
            }
        });
    }
    
    /**
     * Called when we start the server
     * @param event 
     */
    @FXML
    public void createServer(ActionEvent event) {

        if((!txtServerAddress.getText().isEmpty()) && !txtPort.getText().isEmpty()){
                  
            Platform.runLater(() -> {
                FXMLLoader fxmlLoaderServerMain = new FXMLLoader(getClass().getClassLoader().getResource("com/huextrat/views/ServerMain.fxml"));
                try {
                    Parent sceneMain = fxmlLoaderServerMain.load();
                    ServerMainController smc = fxmlLoaderServerMain.<ServerMainController>getController();

                    smc.setController(smc);
                    smc.setServerAdress(txtServerAddress.getText());
                    smc.setServerPort(Integer.parseInt(txtPort.getText()));
                    smc.startServer();

                    Scene sceneLogin = new Scene(sceneMain);

                    Stage stage1 = (Stage) txtPort.getScene().getWindow();
                    stage1.setScene(sceneLogin);
                    stage1.setTitle("FrenchAuction - Server");
                    stage1.show();
                }catch (IOException ex) {
                    Logger.getLogger(ServerLoginController.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
      
            txtServerAddress.setDisable(true);
            txtPort.setDisable(true);
            buttonCreateServer.setDisable(true);
        }
        else if("".equals(txtServerAddress.getText())) {
            txtServerAddress.setPromptText("Enter a valid address please:");
            txtServerAddress.setFocusColor(Color.RED);
            txtServerAddress.requestFocus();
        }
        else if("".equals(txtPort.getText())) {
            txtPort.setPromptText("Enter a valid port please:");
            txtPort.setFocusColor(Color.RED);
            txtPort.requestFocus();
        }    
    }
}
