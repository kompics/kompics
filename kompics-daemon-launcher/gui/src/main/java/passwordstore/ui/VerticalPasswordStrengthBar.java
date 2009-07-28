/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.JComponent;

/**
 * Provides a graphical view of the overall strength of a password.
 *
 * @author sky
 */
public class VerticalPasswordStrengthBar extends JComponent {
    private static final int START = 218;
    private static final int MID = 255;
    private static final int END = 130;
    private static final Color[] RED_COLORS = new Color[] {
            new Color(START, 0, 0),
            new Color(MID, 0, 0),
            new Color(END, 0, 0) };
    private static final Color[] YELLOW_COLORS = new Color[] {
            new Color(START, START, 0),
            new Color(MID, MID, 0),
            new Color(END, END, 0) };
    private static final Color[] GREEN_COLORS = new Color[] {
            new Color(0, START, 0),
            new Color(0, MID, 0),
            new Color(0, END, 0) };

    private float strength;
    
    public void setStrength(float strength) {
        float oldStrength = this.strength;
        this.strength = strength;
        firePropertyChange("strength", oldStrength, strength);
        repaint();
    }
    
    public float getStrength() {
        return strength;
    }

    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        Insets insets = getInsets();
        Graphics2D g2 = (Graphics2D)g.create();
        g2.translate(insets.left, insets.top);
        float strength = getStrength();
        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;
        Paint p1;
        Paint p2;
        int p1Width = w * 5 / 8;
        int barHeight = (int)(strength * (float)h);
        Color[] colors;
        if (strength <= .5f) {
            colors = RED_COLORS;
        } else if (strength <= .8f) {
            colors = YELLOW_COLORS;
        } else {
            colors = GREEN_COLORS;
        }
        g2.setPaint(new GradientPaint(0, 0, colors[0], p1Width, 0, colors[1]));
        g2.fillRect(0, h - barHeight, p1Width, barHeight);
        g2.setPaint(new GradientPaint(p1Width, 0, colors[1], w, 0, colors[2]));
        g2.fillRect(p1Width, h - barHeight, w - p1Width, barHeight);
        g2.dispose();
    }
}
