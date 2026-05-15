package com.mycompany.gogameclient;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;

// Utility class to load and apply our custom retro font
public class FontUtil {
    private static Font customFont;

    // Load the custom font from the resources folder
    public static void loadFont() {
        if (customFont == null) {
            try {
                String[] possiblePaths = {
                        "resources/fonts/Retro-Gaming.ttf",
                        "GoGameClient/resources/fonts/Retro-Gaming.ttf",
                        "src/main/resources/fonts/Retro-Gaming.ttf"
                };
                File fontFile = null;
                for (String path : possiblePaths) {
                    File f = new File(path);
                    if (f.exists()) {
                        fontFile = f;
                        break;
                    }
                }
                if (fontFile != null) {
                    customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);
                } else {
                    System.err.println("Could not find Retro-Gaming.ttf in any of the expected locations.");
                }
            } catch (Exception e) {
                System.out.println("Error loading font: " + e.getMessage());
            }
        }
    }

    // Apply the custom font to a UI component and all its children
    public static void setCustomFont(Component component) {
        if (customFont == null) {
            loadFont();
        }
        if (customFont != null) {
            Font currentFont = component.getFont();
            if (currentFont != null) {
                component.setFont(customFont.deriveFont(currentFont.getStyle(), currentFont.getSize2D()));
            }
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    setCustomFont(child);
                }
            }
        }
    }
}
