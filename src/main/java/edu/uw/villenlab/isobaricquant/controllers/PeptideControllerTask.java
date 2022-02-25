/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uw.villenlab.isobaricquant.controllers;

import java.net.URL;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 *
 * @author julia
 */
public class PeptideControllerTask extends Task<Parent> {

    PeptideController controller;

    @Override
    protected Parent call() throws Exception {
        FXMLLoader loader = new FXMLLoader();
        URL xmlUrl =  this.getClass().getClassLoader().getResource("views/PeptideViewer.fxml");
        //URL xmlUrl = IsobaricQuant.class.getResource("/isobaricquant/views/PeptideViewer.fxml");
        loader.setLocation(xmlUrl);
        Parent root = loader.load();
        controller = loader.getController();
        return root;
    }

    public PeptideController getController() {
        return controller;
    }
}
