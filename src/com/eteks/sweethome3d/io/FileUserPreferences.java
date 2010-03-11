/*
 * FileUserPreferences.java 18 sept 2006
 *
 * Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
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
package com.eteks.sweethome3d.io;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.apple.eio.FileManager;
import com.eteks.sweethome3d.model.CatalogDoorOrWindow;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCatalog;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.IllegalHomonymException;
import com.eteks.sweethome3d.model.LengthUnit;
import com.eteks.sweethome3d.model.PatternsCatalog;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.Sash;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.tools.TemporaryURLContent;
import com.eteks.sweethome3d.tools.URLContent;

/**
 * User preferences initialized from 
 * {@link com.eteks.sweethome3d.io.DefaultUserPreferences default user preferences}
 * and stored in user preferences on local file system. 
 * @author Emmanuel Puybaret
 */
public class FileUserPreferences extends UserPreferences {
  private static final String LANGUAGE                              = "language";
  private static final String UNIT                                  = "unit";
  private static final String FURNITURE_CATALOG_VIEWED_IN_TREE      = "furnitureCatalogViewedInTree";
  private static final String NAVIGATION_PANEL_VISIBLE              = "navigationPanelVisible";
  private static final String MAGNETISM_ENABLED                     = "magnetismEnabled";
  private static final String RULERS_VISIBLE                        = "rulersVisible";
  private static final String GRID_VISIBLE                          = "gridVisible";
  private static final String FURNITURE_VIEWED_FROM_TOP             = "furnitureViewedFromTop";
  private static final String ROOM_FLOOR_COLORED_OR_TEXTURED        = "roomFloorColoredOrTextured";
  private static final String WALL_PATTERN                          = "wallPattern";
  private static final String NEW_WALL_HEIGHT                       = "newHomeWallHeight";
  private static final String NEW_WALL_THICKNESS                    = "newWallThickness";
  private static final String RECENT_HOMES                          = "recentHomes#";
  private static final String IGNORED_ACTION_TIP                    = "ignoredActionTip#";  

  private static final String FURNITURE_NAME                        = "furnitureName#";
  private static final String FURNITURE_CATEGORY                    = "furnitureCategory#";
  private static final String FURNITURE_ICON                        = "furnitureIcon#";
  private static final String FURNITURE_MODEL                       = "furnitureModel#";
  private static final String FURNITURE_WIDTH                       = "furnitureWidth#";
  private static final String FURNITURE_DEPTH                       = "furnitureDepth#";
  private static final String FURNITURE_HEIGHT                      = "furnitureHeight#";
  private static final String FURNITURE_MOVABLE                     = "furnitureMovable#";
  private static final String FURNITURE_DOOR_OR_WINDOW              = "furnitureDoorOrWindow#";
  private static final String FURNITURE_ELEVATION                   = "furnitureElevation#";
  private static final String FURNITURE_COLOR                       = "furnitureColor#";
  private static final String FURNITURE_MODEL_ROTATION              = "furnitureModelRotation#";
  private static final String FURNITURE_BACK_FACE_SHOWN             = "furnitureBackFaceShown#";
  private static final String FURNITURE_ICON_YAW                    = "furnitureIconYaw#";
  private static final String FURNITURE_PROPORTIONAL                = "furnitureProportional#";

  private static final String TEXTURE_NAME                          = "textureName#";
  private static final String TEXTURE_CATEGORY                      = "textureCategory#";
  private static final String TEXTURE_IMAGE                         = "textureImage#";
  private static final String TEXTURE_WIDTH                         = "textureWidth#";
  private static final String TEXTURE_HEIGHT                        = "textureHeight#";

  private static final String FURNITURE_CONTENT_PREFIX              = "Content";
  private static final String TEXTURE_CONTENT_PREFIX                = "TextureContent";

  private static final String LANGUAGE_LIBRARIES_PLUGIN_SUB_FOLDER  = "languages";
  private static final String FURNITURE_LIBRARIES_PLUGIN_SUB_FOLDER = "furniture";
  private static final String TEXTURES_LIBRARIES_PLUGIN_SUB_FOLDER  = "textures";

  private static final Content DUMMY_CONTENT;
  
  private static final String EDITOR_SUB_FOLDER; 
  private static final String APPLICATION_SUB_FOLDER;
  
  private final Map<String, Boolean> ignoredActionTips = new HashMap<String, Boolean>();
  private List<ClassLoader>          resourceClassLoaders;
  private String []                  supportedLanguages;
  
  static {
    Content dummyURLContent = null;
    try {
      dummyURLContent = new URLContent(new URL("file:/dummySweetHome3DContent"));
    } catch (MalformedURLException ex) {
    }
    DUMMY_CONTENT = dummyURLContent;

    // Retrieve sub folders where is stored application data
    ResourceBundle resource = ResourceBundle.getBundle(FileUserPreferences.class.getName());
    if (OperatingSystem.isMacOSX()) {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder.Mac OS X");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder.Mac OS X");
    } else if (OperatingSystem.isWindows()) {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder.Windows");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder.Windows");
    } else {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder");
    }
  }
 
  /**
   * Creates user preferences read either from user preferences in file system, 
   * or from resource files.
   */
  public FileUserPreferences() {
    updateSupportedLanguages();
    
    final Preferences preferences = getPreferences();
    setLanguage(preferences.get(LANGUAGE, getLanguage()));    
    
    // Fill default furniture catalog 
    setFurnitureCatalog(new DefaultFurnitureCatalog(this, getFurnitureLibrariesPluginFolder()));
    // Read additional furniture
    readFurnitureCatalog(preferences);
    
    // Fill default textures catalog 
    setTexturesCatalog(new DefaultTexturesCatalog(this, getTexturesLibrariesPluginFolder()));
    // Read additional textures
    readTexturesCatalog(preferences);

    DefaultUserPreferences defaultPreferences = new DefaultUserPreferences();
    
    // Fill default patterns catalog 
    PatternsCatalog patternsCatalog = defaultPreferences.getPatternsCatalog();
    setPatternsCatalog(patternsCatalog);

    // Read other preferences 
    setUnit(LengthUnit.valueOf(preferences.get(UNIT, 
        defaultPreferences.getLengthUnit().name())));
    setFurnitureCatalogViewedInTree(preferences.getBoolean(FURNITURE_CATALOG_VIEWED_IN_TREE, 
        defaultPreferences.isFurnitureCatalogViewedInTree()));
    setNavigationPanelVisible(preferences.getBoolean(NAVIGATION_PANEL_VISIBLE, 
        defaultPreferences.isNavigationPanelVisible()));
    setMagnetismEnabled(preferences.getBoolean(MAGNETISM_ENABLED, true));
    setRulersVisible(preferences.getBoolean(RULERS_VISIBLE, 
        defaultPreferences.isRulersVisible()));
    setGridVisible(preferences.getBoolean(GRID_VISIBLE, 
        defaultPreferences.isGridVisible()));
    setFurnitureViewedFromTop(preferences.getBoolean(FURNITURE_VIEWED_FROM_TOP, 
        defaultPreferences.isFurnitureViewedFromTop()));
    setFloorColoredOrTextured(preferences.getBoolean(ROOM_FLOOR_COLORED_OR_TEXTURED, 
        defaultPreferences.isRoomFloorColoredOrTextured()));
    try {
      setWallPattern(patternsCatalog.getPattern(preferences.get(WALL_PATTERN, 
          defaultPreferences.getWallPattern().getName())));
    } catch (IllegalArgumentException ex) {
      // Ensure wall pattern always exists even if new patterns are added in future versions
      setWallPattern(defaultPreferences.getWallPattern());
    }
    setNewWallThickness(preferences.getFloat(NEW_WALL_THICKNESS, 
            defaultPreferences.getNewWallThickness()));
    setNewWallHeight(preferences.getFloat(NEW_WALL_HEIGHT,
            defaultPreferences.getNewWallHeight()));    
    setCurrency(defaultPreferences.getCurrency());    
    // Read recent homes list
    List<String> recentHomes = new ArrayList<String>();
    for (int i = 1; i <= 4; i++) {
      String recentHome = preferences.get(RECENT_HOMES + i, null);
      if (recentHome != null) {
        recentHomes.add(recentHome);
      }
    }
    setRecentHomes(recentHomes);
    // Read ignored action tips
    for (int i = 1; ; i++) {
      String ignoredActionTip = preferences.get(IGNORED_ACTION_TIP + i, "");
      if (ignoredActionTip.length() == 0) {
        break;
      } else {
        this.ignoredActionTips.put(ignoredActionTip, true);
      }
    }
    
    addPropertyChangeListener(Property.LANGUAGE, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          updateDefaultCatalogs();
        }
      });
  }

  /**
   * Updates the default supported languages with languages available in plugin folder. 
   */
  private void updateSupportedLanguages() {
    List<ClassLoader> resourceClassLoaders = new ArrayList<ClassLoader>();
    String [] defaultSupportedLanguages = super.getSupportedLanguages();
    Set<String> supportedLanguages = new TreeSet<String>(Arrays.asList(defaultSupportedLanguages));
   
    File languageLibrariesPluginFolder = getLanguageLibrariesPluginFolder();
    if (languageLibrariesPluginFolder != null) {
      // Try to load sh3l files from language plugin folder
      File [] pluginLanguageLibraryFiles = languageLibrariesPluginFolder.listFiles(new FileFilter () {
        public boolean accept(File pathname) {
          return pathname.isFile();
        }
      });
      
      if (pluginLanguageLibraryFiles != null) {
        // Treat language files in reverse order so file named with a date or a version 
        // will be taken into account from most recent to least recent
        Arrays.sort(pluginLanguageLibraryFiles, Collections.reverseOrder());
        for (File pluginLanguageLibraryFile : pluginLanguageLibraryFiles) {
          try {
            Set<String> languages = getLanguages(pluginLanguageLibraryFile);
            if (!languages.isEmpty()) {
              supportedLanguages.addAll(languages);
              URL pluginFurnitureCatalogUrl = pluginLanguageLibraryFile.toURI().toURL();
              resourceClassLoaders.add(new URLClassLoader(new URL [] {pluginFurnitureCatalogUrl}));
            }
          } catch (IOException ex) {
            // Ignore malformed files
          }
        }
      }
    }
    
    // Give less priority to default class loader
    resourceClassLoaders.addAll(super.getResourceClassLoaders());
    this.resourceClassLoaders = Collections.unmodifiableList(resourceClassLoaders);
    if (defaultSupportedLanguages.length < supportedLanguages.size()) {
      this.supportedLanguages = supportedLanguages.toArray(new String [supportedLanguages.size()]);
    } else {
      this.supportedLanguages = defaultSupportedLanguages;
    }
  }

  /**
   * Returns the languages included in the given language library file.
   */
  private Set<String> getLanguages(File languageLibraryFile) throws IOException {
    Set<String> languages = new LinkedHashSet<String>();
    ZipInputStream zipIn = null;
    try {
      // Search if zip file contains some *_xx.properties or *_xx_xx.properties files
      zipIn = new ZipInputStream(new FileInputStream(languageLibraryFile));
      for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
        String zipEntryName = entry.getName();
        int underscoreIndex = zipEntryName.indexOf('_');
        if (underscoreIndex != -1) {
          int extensionIndex = zipEntryName.lastIndexOf(".properties");
          if (extensionIndex != -1 && underscoreIndex < extensionIndex - 2) {
            String language = zipEntryName.substring(underscoreIndex + 1, extensionIndex);
            int countrySeparator = language.indexOf('_');
            if (countrySeparator == 2
                && language.length() == 5) {
              languages.add(language);
            } else if (language.length() == 2) {
              languages.add(language);
            }
          }
        }
      }
      return languages;
    } finally {
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }
  
  /**
   * Returns the default languages supported in Sweet Home 3D and languages in plugin libraries.
   */
  @Override
  public String [] getSupportedLanguages() {    
    return this.supportedLanguages;
  }
  
  /**
   * Returns the default class loader of user preferences and class loader that
   * give access to resources in language libraries plugin folder. 
   */
  @Override
  protected List<ClassLoader> getResourceClassLoaders() {
    if (this.resourceClassLoaders == null) {
      // Return default class loader if this method is called from super class constructor 
      return super.getResourceClassLoaders();
    } else {
      return this.resourceClassLoaders;
    }
  }
  
  /**
   * Reloads furniture and textures default catalogs.
   */
  private void updateDefaultCatalogs() {
    // Delete default pieces of current furniture catalog          
    FurnitureCatalog furnitureCatalog = getFurnitureCatalog();
    for (FurnitureCategory category : furnitureCatalog.getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (!piece.isModifiable()) {
          furnitureCatalog.delete(piece);
        }
      }
    }
    // Read again default furniture and textures catalogs with new default locale
    // Add default pieces that don't have homonym among user catalog
    FurnitureCatalog defaultFurnitureCatalog = 
        new DefaultFurnitureCatalog(this, getFurnitureLibrariesPluginFolder());
    for (FurnitureCategory category : defaultFurnitureCatalog.getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        try {
          furnitureCatalog.add(category, piece);
        } catch (IllegalHomonymException ex) {
          // Ignore pieces that have the same name as an existing piece
        }
      }
    }
    
    // Delete default textures of current textures catalog          
    TexturesCatalog texturesCatalog = getTexturesCatalog();
    for (TexturesCategory category : texturesCatalog.getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (!texture.isModifiable()) {
          texturesCatalog.delete(texture);
        }
      }
    }
    // Add default textures that don't have homonym among user catalog
    TexturesCatalog defaultTexturesCatalog = 
        new DefaultTexturesCatalog(this, getTexturesLibrariesPluginFolder());
    for (TexturesCategory category : defaultTexturesCatalog.getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        try {
          texturesCatalog.add(category, texture);
        } catch (IllegalHomonymException ex) {
          // Ignore textures that have the same name as an existing piece
        }
      }
    }
  }

  /**
   * Read furniture catalog from preferences.
   */
  private void readFurnitureCatalog(Preferences preferences) {
    for (int i = 1; ; i++) {
      String name = preferences.get(FURNITURE_NAME + i, null);
      if (name == null) {
        // Stop the loop when a key furnitureName# doesn't exist
        break;
      }
      String category = preferences.get(FURNITURE_CATEGORY + i, "");
      Content icon  = getContent(preferences, FURNITURE_ICON + i);
      Content model = getContent(preferences, FURNITURE_MODEL + i);
      float width = preferences.getFloat(FURNITURE_WIDTH + i, 0.1f);
      float depth = preferences.getFloat(FURNITURE_DEPTH + i, 0.1f);
      float height = preferences.getFloat(FURNITURE_HEIGHT + i, 0.1f);
      boolean movable = preferences.getBoolean(FURNITURE_MOVABLE + i, false);
      boolean doorOrWindow = preferences.getBoolean(FURNITURE_DOOR_OR_WINDOW + i, false);
      float elevation = preferences.getFloat(FURNITURE_ELEVATION + i, 0);
      String colorString = preferences.get(FURNITURE_COLOR + i, null);
      Integer color = colorString != null 
          ? Integer.valueOf(colorString) : null; 
      float [][] modelRotation = getModelRotation(preferences, FURNITURE_MODEL_ROTATION + i);
      boolean backFaceShown = preferences.getBoolean(FURNITURE_BACK_FACE_SHOWN + i, false);
      float iconYaw = preferences.getFloat(FURNITURE_ICON_YAW + i, 0);
      boolean proportional = preferences.getBoolean(FURNITURE_PROPORTIONAL + i, true);

      FurnitureCategory pieceCategory = new FurnitureCategory(category);
      CatalogPieceOfFurniture piece;
      if (doorOrWindow) {
        piece = new CatalogDoorOrWindow(name, icon, model,
            width, depth, height, elevation, movable, 1, 0, new Sash [0],
            color, modelRotation, backFaceShown, iconYaw, proportional);
      } else {
        piece = new CatalogPieceOfFurniture(name, icon, model,
            width, depth, height, elevation, movable, 
            color, modelRotation, backFaceShown, iconYaw, proportional);
      }
      try {        
        getFurnitureCatalog().add(pieceCategory, piece);
      } catch (IllegalHomonymException ex) {
        // If a piece with same name and category already exists in furniture catalog
        // replace the existing piece by the new one
        List<FurnitureCategory> categories = getFurnitureCatalog().getCategories();
        int categoryIndex = Collections.binarySearch(categories, pieceCategory);
        List<CatalogPieceOfFurniture> furniture = categories.get(categoryIndex).getFurniture();
        int existingPieceIndex = Collections.binarySearch(furniture, piece);        
        getFurnitureCatalog().delete(furniture.get(existingPieceIndex));
        
        getFurnitureCatalog().add(pieceCategory, piece);
      }
    }
  }  

  /**
   * Returns model rotation parsed from key value.
   */
  private float [][] getModelRotation(Preferences preferences, String key) {
    String modelRotationString = preferences.get(key, null);
    if (modelRotationString == null) {
      return new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    } else {
      String [] values = modelRotationString.split(" ", 9);
      if (values.length != 9) {
        return new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
      } else {
        try {
          return new float [][] {{Float.parseFloat(values [0]), 
                                  Float.parseFloat(values [1]), 
                                  Float.parseFloat(values [2])}, 
                                 {Float.parseFloat(values [3]), 
                                  Float.parseFloat(values [4]), 
                                  Float.parseFloat(values [5])}, 
                                 {Float.parseFloat(values [6]), 
                                  Float.parseFloat(values [7]), 
                                  Float.parseFloat(values [8])}};
        } catch (NumberFormatException ex) {
          return new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        }
      }
    }
  }
  
  /**
   * Returns a content instance from the resource file value of key.
   */
  private Content getContent(Preferences preferences, String key) {
    String content = preferences.get(key, null);
    if (content != null) {
      try {
        return new URLContent(new URL(content));
      } catch (MalformedURLException ex) {
        // Return DUMMY_CONTENT for incorrect URL
      } 
    }
    return DUMMY_CONTENT;
  }
  
  /**
   * Read textures catalog from preferences.
   */
  private void readTexturesCatalog(Preferences preferences) {
    for (int i = 1; ; i++) {
      String name = preferences.get(TEXTURE_NAME + i, null);
      if (name == null) {
        // Stop the loop when a key textureName# doesn't exist
        break;
      }
      String category = preferences.get(TEXTURE_CATEGORY + i, "");
      Content image = getContent(preferences, TEXTURE_IMAGE + i);
      float width = preferences.getFloat(TEXTURE_WIDTH + i, 0.1f);
      float height = preferences.getFloat(TEXTURE_HEIGHT + i, 0.1f);

      TexturesCategory textureCategory = new TexturesCategory(category);
      CatalogTexture texture = new CatalogTexture(name, image, width, height, true);
      try {        
        getTexturesCatalog().add(textureCategory, texture);
      } catch (IllegalHomonymException ex) {
        // If a texture with same name and category already exists in textures catalog
        // replace the existing texture by the new one
        List<TexturesCategory> categories = getTexturesCatalog().getCategories();
        int categoryIndex = Collections.binarySearch(categories, textureCategory);
        List<CatalogTexture> textures = categories.get(categoryIndex).getTextures();
        int existingTextureIndex = Collections.binarySearch(textures, texture);        
        getTexturesCatalog().delete(textures.get(existingTextureIndex));
        
        getTexturesCatalog().add(textureCategory, texture);
      }
    }
  }  

  /**
   * Writes user preferences in current user preferences in system.
   */
  @Override
  public void write() throws RecorderException {
    Preferences preferences = getPreferences();

    writeFurnitureCatalog(preferences);
    writeTexturesCatalog(preferences);

    // Write other preferences 
    preferences.put(LANGUAGE, getLanguage());
    preferences.put(UNIT, getLengthUnit().name());   
    preferences.putBoolean(FURNITURE_CATALOG_VIEWED_IN_TREE, isFurnitureCatalogViewedInTree());
    preferences.putBoolean(NAVIGATION_PANEL_VISIBLE, isNavigationPanelVisible());
    preferences.putBoolean(MAGNETISM_ENABLED, isMagnetismEnabled());
    preferences.putBoolean(RULERS_VISIBLE, isRulersVisible());
    preferences.putBoolean(GRID_VISIBLE, isGridVisible());
    preferences.putBoolean(FURNITURE_VIEWED_FROM_TOP, isFurnitureViewedFromTop());
    preferences.putBoolean(ROOM_FLOOR_COLORED_OR_TEXTURED, isRoomFloorColoredOrTextured());
    preferences.put(WALL_PATTERN, getWallPattern().getName());
    preferences.putFloat(NEW_WALL_THICKNESS, getNewWallThickness());   
    preferences.putFloat(NEW_WALL_HEIGHT, getNewWallHeight());
    // Write recent homes list
    int i = 1;
    for (Iterator<String> it = getRecentHomes().iterator(); it.hasNext() && i <= 4; i ++) {
      preferences.put(RECENT_HOMES + i, it.next());
    }
    // Remove obsolete keys
    for ( ; i <= 4; i++) {
      preferences.remove(RECENT_HOMES + i);
    }
    // Write ignored action tips
    i = 1;
    for (Iterator<Map.Entry<String, Boolean>> it = this.ignoredActionTips.entrySet().iterator();
         it.hasNext(); ) {
      Entry<String, Boolean> ignoredActionTipEntry = it.next();
      if (ignoredActionTipEntry.getValue()) {
        preferences.put(IGNORED_ACTION_TIP + i++, ignoredActionTipEntry.getKey());
      } 
    }
    // Remove obsolete keys
    for ( ; i <= this.ignoredActionTips.size(); i++) {
      preferences.remove(IGNORED_ACTION_TIP + i);
    }
    
    try {
      // Write preferences 
      preferences.sync();
    } catch (BackingStoreException ex) {
      throw new RecorderException("Couldn't write preferences", ex);
    }
  }

  /**
   * Writes furniture catalog in <code>preferences</code>.
   */
  private void writeFurnitureCatalog(Preferences preferences) throws RecorderException {
    final Set<URL> furnitureContentURLs = new HashSet<URL>();
    int i = 1;
    for (FurnitureCategory category : getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (piece.isModifiable()) {
          preferences.put(FURNITURE_NAME + i, piece.getName());
          preferences.put(FURNITURE_CATEGORY + i, category.getName());
          putContent(preferences, FURNITURE_ICON + i, piece.getIcon(), 
              FURNITURE_CONTENT_PREFIX, furnitureContentURLs);
          putContent(preferences, FURNITURE_MODEL + i, piece.getModel(), 
              FURNITURE_CONTENT_PREFIX, furnitureContentURLs);
          preferences.putFloat(FURNITURE_WIDTH + i, piece.getWidth());
          preferences.putFloat(FURNITURE_DEPTH + i, piece.getDepth());
          preferences.putFloat(FURNITURE_HEIGHT + i, piece.getHeight());
          preferences.putBoolean(FURNITURE_MOVABLE + i, piece.isMovable());
          preferences.putBoolean(FURNITURE_DOOR_OR_WINDOW + i, piece.isDoorOrWindow());
          preferences.putFloat(FURNITURE_ELEVATION + i, piece.getElevation());
          if (piece.getColor() == null) {
            preferences.remove(FURNITURE_COLOR + i);
          } else {
            preferences.put(FURNITURE_COLOR + i, String.valueOf(piece.getColor()));
          }
          float [][] modelRotation = piece.getModelRotation();
          preferences.put(FURNITURE_MODEL_ROTATION + i, 
              modelRotation[0][0] + " " + modelRotation[0][1] + " " + modelRotation[0][2] + " "
              + modelRotation[1][0] + " " + modelRotation[1][1] + " " + modelRotation[1][2] + " "
              + modelRotation[2][0] + " " + modelRotation[2][1] + " " + modelRotation[2][2]);
          preferences.putBoolean(FURNITURE_BACK_FACE_SHOWN + i, piece.isBackFaceShown());
          preferences.putFloat(FURNITURE_ICON_YAW + i, piece.getIconYaw());
          preferences.putBoolean(FURNITURE_PROPORTIONAL + i, piece.isProportional());
          i++;
        }
      }
    }
    // Remove obsolete keys
    for ( ; preferences.get(FURNITURE_NAME + i, null) != null; i++) {
      preferences.remove(FURNITURE_NAME + i);
      preferences.remove(FURNITURE_CATEGORY + i);
      preferences.remove(FURNITURE_ICON + i);
      preferences.remove(FURNITURE_MODEL + i);
      preferences.remove(FURNITURE_WIDTH + i);
      preferences.remove(FURNITURE_DEPTH + i);
      preferences.remove(FURNITURE_HEIGHT + i);
      preferences.remove(FURNITURE_MOVABLE + i);
      preferences.remove(FURNITURE_DOOR_OR_WINDOW + i);
      preferences.remove(FURNITURE_ELEVATION + i);
      preferences.remove(FURNITURE_COLOR + i);
      preferences.remove(FURNITURE_MODEL_ROTATION + i);
      preferences.remove(FURNITURE_BACK_FACE_SHOWN + i);
      preferences.remove(FURNITURE_ICON_YAW + i);
      preferences.remove(FURNITURE_PROPORTIONAL + i);
    }
    deleteObsoleteContent(furnitureContentURLs, FURNITURE_CONTENT_PREFIX);
  }

  /**
   * Writes textures catalog in <code>preferences</code>.
   */
  private void writeTexturesCatalog(Preferences preferences) throws RecorderException {
    final Set<URL> texturesContentURLs = new HashSet<URL>();
    int i = 1;
    for (TexturesCategory category : getTexturesCatalog().getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (texture.isModifiable()) {
          preferences.put(TEXTURE_NAME + i, texture.getName());
          preferences.put(TEXTURE_CATEGORY + i, category.getName());
          putContent(preferences, TEXTURE_IMAGE + i, texture.getImage(), 
              TEXTURE_CONTENT_PREFIX, texturesContentURLs);
          preferences.putFloat(TEXTURE_WIDTH + i, texture.getWidth());
          preferences.putFloat(TEXTURE_HEIGHT + i, texture.getHeight());
          i++;
        }
      }
    }
    // Remove obsolete keys
    for ( ; preferences.get(TEXTURE_NAME + i, null) != null; i++) {
      preferences.remove(TEXTURE_NAME + i);
      preferences.remove(TEXTURE_CATEGORY + i);
      preferences.remove(TEXTURE_IMAGE + i);
      preferences.remove(TEXTURE_WIDTH + i);
      preferences.remove(TEXTURE_HEIGHT + i);
    }
    deleteObsoleteContent(texturesContentURLs, TEXTURE_CONTENT_PREFIX);
  }

  /**
   * Writes <code>key</code> <code>content</code> in <code>preferences</code>.
   */
  private void putContent(Preferences preferences, String key, 
                          Content content, String contentPrefix,
                          Set<URL> furnitureContentURLs) throws RecorderException {
    if (content instanceof TemporaryURLContent) {
      URLContent urlContent = (URLContent)content;
      URLContent copiedContent;
      if (urlContent.isJAREntry()) {
        try {
          // If content is a JAR entry copy the content of its URL and rebuild a new URL content from 
          // this copy and the entry name
          copiedContent = copyToApplicationURLContent(new URLContent(urlContent.getJAREntryURL()), contentPrefix);
          copiedContent = new URLContent(new URL("jar:" + copiedContent.getURL() + "!/" + urlContent.getJAREntryName()));
        } catch (MalformedURLException ex) {
          // Shouldn't happen
          throw new RecorderException("Can't build URL", ex);
        }
      } else {
        copiedContent = copyToApplicationURLContent(urlContent, contentPrefix);
      }
      putContent(preferences, key, copiedContent, contentPrefix, furnitureContentURLs);
    } else if (content instanceof URLContent) {
      URLContent urlContent = (URLContent)content;
      preferences.put(key, urlContent.getURL().toString());
      // Add to furnitureContentURLs the URL to the application file
      if (urlContent.isJAREntry()) {
        furnitureContentURLs.add(urlContent.getJAREntryURL());
      } else {
        furnitureContentURLs.add(urlContent.getURL());
      }
    } else {
      putContent(preferences, key, copyToApplicationURLContent(content, contentPrefix), 
          contentPrefix, furnitureContentURLs);
    }
  }

  /**
   * Returns a content object that references a copy of <code>content</code> in 
   * user application folder.
   */
  private URLContent copyToApplicationURLContent(Content content, 
                                                 String contentPrefix) throws RecorderException {
    InputStream tempIn = null;
    OutputStream tempOut = null;
    try {
      File applicationFile = createApplicationFile(contentPrefix);
      tempIn = content.openStream();
      tempOut = new FileOutputStream(applicationFile);
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = tempIn.read(buffer)) != -1) {
        tempOut.write(buffer, 0, size);
      }
      return new URLContent(applicationFile.toURI().toURL());
    } catch (IOException ex) {
      throw new RecorderException("Can't save content", ex);
    } finally {
      try {
        if (tempIn != null) {
          tempIn.close();
        }
        if (tempOut != null) {
          tempOut.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close files", ex);
      }
    }
  }

  /**
   * Returns a new file in user application folder.
   */
  private File createApplicationFile(String filePrefix) throws IOException {
    File applicationFolder = getApplicationFolder();
    // Create application folder if it doesn't exist
    if (!applicationFolder.exists()
        && !applicationFolder.mkdirs()) {
      throw new IOException("Couldn't create " + applicationFolder);
    }
    // Return a new file in application folder
    return File.createTempFile(filePrefix, ".pref", applicationFolder);
  }

  /**
   * Returns the folder where language libraries files must be placed 
   * or <code>null</code> if that folder can't be retrieved.
   */
  private File getLanguageLibrariesPluginFolder() {
    try {
      return new File(getApplicationFolder(), LANGUAGE_LIBRARIES_PLUGIN_SUB_FOLDER);
    } catch (IOException ex) {
      return null;
    }
  }

  /**
   * Returns the folder where furniture catalog files must be placed 
   * or <code>null</code> if that folder can't be retrieved.
   */
  private File getFurnitureLibrariesPluginFolder() {
    try {
      return new File(getApplicationFolder(), FURNITURE_LIBRARIES_PLUGIN_SUB_FOLDER);
    } catch (IOException ex) {
      return null;
    }
  }

  /**
   * Returns the folder where texture catalog files must be placed 
   * or <code>null</code> if that folder can't be retrieved.
   */
  private File getTexturesLibrariesPluginFolder() {
    try {
      return new File(getApplicationFolder(), TEXTURES_LIBRARIES_PLUGIN_SUB_FOLDER);
    } catch (IOException ex) {
      return null;
    }
  }

  /**
   * Returns Sweet Home 3D application folder. 
   */
  public File getApplicationFolder() throws IOException {
    File userApplicationFolder; 
    if (OperatingSystem.isMacOSX()) {
      userApplicationFolder = new File(MacOSXFileManager.getApplicationSupportFolder());
    } else if (OperatingSystem.isWindows()) {
      userApplicationFolder = new File(System.getProperty("user.home"), "Application Data");
      // If user Application Data directory doesn't exist, use user home
      if (!userApplicationFolder.exists()) {
        userApplicationFolder = new File(System.getProperty("user.home"));
      }
    } else { 
      // Unix
      userApplicationFolder = new File(System.getProperty("user.home"));
    }
    return new File(userApplicationFolder, 
        EDITOR_SUB_FOLDER + File.separator + APPLICATION_SUB_FOLDER);
  }

  /**
   * Deletes from application folder the content files starting by <code>contentPrefix</code>
   * that don't belong to <code>contentURLs</code>. 
   */
  private void deleteObsoleteContent(final Set<URL> contentURLs, 
                                     final String contentPrefix) throws RecorderException {
    // Search obsolete contents
    File applicationFolder;
    try {
      applicationFolder = getApplicationFolder();
    } catch (IOException ex) {
      throw new RecorderException("Can't access to application folder");
    }
    File [] obsoleteContentFiles = applicationFolder.listFiles(
        new FileFilter() {
          public boolean accept(File applicationFile) {
            try {
              URL toURL = applicationFile.toURI().toURL();
              return applicationFile.getName().startsWith(contentPrefix)
                 && !contentURLs.contains(toURL);
            } catch (MalformedURLException ex) {
              return false;
            }
          }
        });
    if (obsoleteContentFiles != null) {
      // Remove obsolete contents
      for (File file : obsoleteContentFiles) {
        if (!file.delete()) {
          throw new RecorderException("Couldn't delete file " + file);
        }
      }
    }
  }
  
  /**
   * File manager class that accesses to Mac OS X specifics.
   * Do not invoke methods of this class without checking first if 
   * <code>os.name</code> System property is <code>Mac OS X</code>.
   * This class requires some classes of <code>com.apple.eio</code> package  
   * to compile.
   */
  private static class MacOSXFileManager {
    public static String getApplicationSupportFolder() throws IOException {
      // Find application support folder (0x61737570) for user domain (-32763)
      return FileManager.findFolder((short)-32763, 0x61737570);
    }
  }
  
  /**
   * Returns Java preferences for current system user.
   */
  protected Preferences getPreferences() {
    return Preferences.userNodeForPackage(FileUserPreferences.class);
  }

  /**
   * Sets which action tip should be ignored.
   */
  @Override
  public void setActionTipIgnored(String actionKey) {   
    this.ignoredActionTips.put(actionKey, true);
    super.setActionTipIgnored(actionKey);
  }
  
  /**
   * Returns whether an action tip should be ignored or not. 
   */
  @Override
  public boolean isActionTipIgnored(String actionKey) {
    Boolean ignoredActionTip = this.ignoredActionTips.get(actionKey);
    return ignoredActionTip != null && ignoredActionTip.booleanValue();
  }
  
  /**
   * Resets the display flag of action tips.
   */
  @Override
  public void resetIgnoredActionTips() {
    for (Iterator<Map.Entry<String, Boolean>> it = this.ignoredActionTips.entrySet().iterator();
         it.hasNext(); ) {
      Entry<String, Boolean> ignoredActionTipEntry = it.next();
      ignoredActionTipEntry.setValue(false);
    }
    super.resetIgnoredActionTips();
  }
  
  /**
   * Returns <code>true</code> if the given language library exists.
   */
  public boolean languageLibraryExists(String name) throws RecorderException {
    File languageLibrariesFolder = getLanguageLibrariesPluginFolder();
    if (languageLibrariesFolder == null) {
      throw new RecorderException("Can't access to language libraries plugin folder");
    } else {
      String libraryFileName = new File(name).getName();
      return new File(languageLibrariesFolder, libraryFileName).exists();
    }
  }
  
  /**
   * Adds <code>languageLibraryName</code> to language catalog  
   * to make the language library it contains available to supported languages.
   */
  public void addLanguageLibrary(String languageLibraryName) throws RecorderException {
    try {
      File languageLibrariesPluginFolder = getLanguageLibrariesPluginFolder();
      if (languageLibrariesPluginFolder == null) {
        throw new RecorderException("Can't access to language libraries plugin folder");
      }
      File languageLibraryFile = new File(languageLibraryName);
      copyToLibraryFolder(languageLibraryFile, languageLibrariesPluginFolder);
      updateSupportedLanguages();
      // Switch automatically to the first language contained in library
      Set<String> languages = getLanguages(languageLibraryFile);
      if (!languages.isEmpty()) {
        setLanguage(languages.iterator().next());
      }
    } catch (IOException ex) {
      throw new RecorderException(
          "Can't write " + languageLibraryName +  " in language libraries plugin folder", ex);
    }
  }

  /**
   * Returns <code>true</code> if the given furniture library file exists in furniture libraries folder.
   * @param name the name of the resource to check
   */
  @Override
  public boolean furnitureLibraryExists(String name) throws RecorderException {
    File furnitureLibrariesPluginFolder = getFurnitureLibrariesPluginFolder();
    if (furnitureLibrariesPluginFolder == null) {
      throw new RecorderException("Can't access to furniture libraries plugin folder");
    } else {
      String libraryFileName = new File(name).getName();
      return new File(furnitureLibrariesPluginFolder, libraryFileName).exists();
    }
  }

  /**
   * Adds the file <code>furnitureLibraryName</code> to furniture libraries folder 
   * to make the furniture library available to catalog.
   */
  @Override
  public void addFurnitureLibrary(String furnitureLibraryName) throws RecorderException {
    try {
      File furnitureLibrariesPluginFolder = getFurnitureLibrariesPluginFolder();
      if (furnitureLibrariesPluginFolder == null) {
        throw new RecorderException("Can't access to furniture libraries plugin folder");
      }
      copyToLibraryFolder(new File(furnitureLibraryName), furnitureLibrariesPluginFolder);
      updateDefaultCatalogs();
    } catch (IOException ex) {
      throw new RecorderException(
          "Can't write " + furnitureLibraryName +  " in furniture libraries plugin folder", ex);
    }
  }

  /**
   * Returns <code>true</code> if the given textures library file exists in textures libraries folder.
   * @param name the name of the resource to check
   */
  @Override
  public boolean texturesLibraryExists(String name) throws RecorderException {
    File texturesLibrariesPluginFolder = getTexturesLibrariesPluginFolder();
    if (texturesLibrariesPluginFolder == null) {
      throw new RecorderException("Can't access to textures libraries plugin folder");
    } else {
      String libraryFileName = new File(name).getName();
      return new File(texturesLibrariesPluginFolder, libraryFileName).exists();
    }
  }

  /**
   * Adds the file <code>texturesLibraryName</code> to textures libraries folder 
   * to make the textures library available to catalog.
   */
  @Override
  public void addTexturesLibrary(String texturesLibraryName) throws RecorderException {
    try {
      File texturesLibrariesPluginFolder = getTexturesLibrariesPluginFolder();
      if (texturesLibrariesPluginFolder == null) {
        throw new RecorderException("Can't access to textures libraries plugin folder");
      }
      copyToLibraryFolder(new File(texturesLibraryName), texturesLibrariesPluginFolder);
      updateDefaultCatalogs();
    } catch (IOException ex) {
      throw new RecorderException(
          "Can't write " + texturesLibraryName +  " in textures libraries plugin folder", ex);
    }
  }

  /**
   * Copies a library file to a folder.
   */
  private void copyToLibraryFolder(File libraryFile, File folder) throws IOException {
    String libraryFileName = libraryFile.getName();
    File destinationFile = new File(folder, libraryFileName);
    
    InputStream tempIn = null;
    OutputStream tempOut = null;
    try {
      tempIn = new BufferedInputStream(new FileInputStream(libraryFile));
      folder.mkdirs();
      tempOut = new FileOutputStream(destinationFile);          
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = tempIn.read(buffer)) != -1) {
        tempOut.write(buffer, 0, size);
      }
    } finally {
      if (tempIn != null) {
        tempIn.close();
      }
      if (tempOut != null) {
        tempOut.close();
      }
    }
  }
}
