/*
 * AppletContentManager.java 13 Oct. 2008
 *
 * Copyright (c) 2008 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.applet;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import com.eteks.sweethome3d.model.HomeRecorder;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.swing.FileContentManager;

/**
 * Content manager for Sweet Home 3D files stored on server.
 * @author Emmanuel Puybaret
 */
public class AppletContentManager extends FileContentManager {
  private final HomeRecorder recorder;

  public AppletContentManager(HomeRecorder recorder) {
    this.recorder = recorder;  
  }
  
  /**
   * Returns the name of the content in parameter.
   */
  public String getPresentationName(String contentName, 
                                    ContentType contentType) {
    if (contentType == ContentType.SWEET_HOME_3D) {
      return contentName;
    } else {
      return super.getPresentationName(contentName, contentType);
    }    
  }
  
  /**
   * Returns <code>true</code> if the content name in parameter is accepted
   * for <code>contentType</code>.
   */
  public boolean isAcceptable(String contentName, 
                              ContentType contentType) {
    if (contentType == ContentType.SWEET_HOME_3D) {
      return true;
    } else {
      return super.isAcceptable(contentName, contentType);
    }    
  }
  
  /**
   * Returns the name chosen by user with an open dialog.
   * @return the name or <code>null</code> if user canceled its choice.
   */
  public String showOpenDialog(String      dialogTitle,
                               ContentType contentType) {
    if (contentType == ContentType.SWEET_HOME_3D) {
      ResourceBundle resource = ResourceBundle.getBundle(AppletContentManager.class.getName());
      Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      String [] availableHomes = null;
      if (this.recorder instanceof HomeAppletRecorder) {
        try {
          availableHomes = ((HomeAppletRecorder)this.recorder).getAvailableHomes();
        } catch (RecorderException ex) {
          String errorMessage = resource.getString("showOpenDialog.availableHomesError");
          showError(activeWindow, errorMessage);
          return null;
        }
      }    
      
      if (availableHomes != null && availableHomes.length == 0) {
        String message = resource.getString("showOpenDialog.noAvailableHomes");
        JOptionPane.showMessageDialog(activeWindow, 
            message, getDefaultDialogTitle(false), JOptionPane.INFORMATION_MESSAGE);
        return null;
      } else {
        String message = resource.getString("showOpenDialog.message");
        return (String)JOptionPane.showInputDialog(activeWindow, 
            message, getDefaultDialogTitle(false), JOptionPane.QUESTION_MESSAGE, null, availableHomes, null);
      }
    } else {
      return super.showOpenDialog(dialogTitle, contentType);
    }
  }
  
  /**
   * Returns the name chosen by user with a save dialog.
   * If this name already exists, the user will be prompted whether 
   * he wants to overwrite this existing name. 
   * @return the chosen name or <code>null</code> if user canceled its choice.
   */
  public String showSaveDialog(String      dialogTitle,
                               ContentType contentType,
                               String      name) {
    if (contentType == ContentType.SWEET_HOME_3D) {
      ResourceBundle resource = ResourceBundle.getBundle(AppletContentManager.class.getName());
      Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      String message = resource.getString("showSaveDialog.message");
      String savedName = (String)JOptionPane.showInputDialog(activeWindow, 
          message, getDefaultDialogTitle(true), JOptionPane.QUESTION_MESSAGE, null, null, name); 
  
      // If the name exists, prompt user if he wants to overwrite it
      try {
        if (this.recorder.exists(savedName)
            && !confirmOverwrite(activeWindow, savedName)) {
          return showSaveDialog(dialogTitle, contentType, savedName);
        }
        return savedName;
      } catch (RecorderException ex) {
        String errorMessage = resource.getString("showSaveDialog.checkHomeError");
        showError(activeWindow, errorMessage);
        return null;
      }
    } else {
      return super.showSaveDialog(dialogTitle, contentType, name);
    }
  }
  
  /**
   * Shows the given <code>message</code> in an error message dialog. 
   */
  private void showError(Component parent, String message) {
    ResourceBundle resource = ResourceBundle.getBundle(AppletContentManager.class.getName());
    String title = resource.getString("showError.title");
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);    
  }
  
  /**
   * Returns default dialog title.
   */
  private String getDefaultDialogTitle(boolean save) {
    ResourceBundle resource = ResourceBundle.getBundle(FileContentManager.class.getName());
    if (save) {
      return resource.getString("saveDialog.title");
    } else {
      return resource.getString("openDialog.title");
    }
  }
}