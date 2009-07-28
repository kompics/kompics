/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Provides a visualization of the distribution of the characters in
 * a password. Some liberties have been taken to make this is little more
 * wizzy then useful.
 *
 * @author sky
 */
public class PasswordVisualizer extends JPanel {
    private enum Type {
        UPPER_CASE,
        LOWER_CASE,
        DIGIT,
        OTHER,
        NONE
    };
    
    private Column[] columns;
    private Column[] targetColumns;
    private int indicatorHeight;
    private int indicatorWidth;
    private int indicatorXSpacing;
    private int indicatorYSpacing;
    private int maxColumns;
    private int maxRows;
    private String password;
    private Timer timer;

    private Color digitColor;

    private Color upperCaseColor;

    private Color lowerCaseColor;

    private Color otherColor;

    private boolean animatesTransitions;

    public PasswordVisualizer() {
        maxRows = 8;
        indicatorXSpacing = 2;
        indicatorYSpacing = 2;
        indicatorWidth = 12;
        indicatorHeight = 4;
        setMaxColumns(12);
    }
    
    public void setDigitColor(Color color) {
        this.digitColor = color;
        repaint();
    }
    
    public Color getDigitColor() {
        return digitColor;
    }
    
    public void setOtherColor(Color color) {
        this.otherColor = color;
        repaint();
    }
    
    public Color getOtherColor() {
        return otherColor;
    }
    
    public void setLowerCaseColor(Color color) {
        this.lowerCaseColor = color;
        repaint();
    }
    
    public Color getLowerCaseColor() {
        return lowerCaseColor;
    }
    
    public void setUpperCaseColor(Color color) {
        this.upperCaseColor = color;
        repaint();
    }
    
    public Color getUpperCaseColor() {
        return upperCaseColor;
    }
    
    public void setIndicatorHeight(int height) {
        this.indicatorHeight = height;
        update();
    }
    
    public void setIndicatorWidth(int width) {
        indicatorWidth = width;
        update();
    }
    
    public void setIndicatorXSpacing(int spacing) {
        indicatorXSpacing = spacing;
        update();
    }
    
    public void setIndicatorYSpacing(int spacing) {
        indicatorYSpacing = spacing;
        update();
    }
    
    public void setMaxColumns(int max) {
        if (max < 1) {
            throw new IllegalArgumentException();
        }
        maxColumns = max;
        columns = new Column[max];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new Column();
        }
        stopTimer();
        update();
    }
    
    public void setMaxRows(int rows) {
        if (rows < 1) {
            throw new IllegalArgumentException();
        }
        maxRows = rows;
        update();
    }
    
    public void setPassword(String password) {
        if ((this.password == null && password != null) ||
                (this.password != null && password == null) ||
                (this.password != null && !this.password.equals(password))) {
            String oldPassword = password;
            this.password = password;
            update();
            firePropertyChange("password", oldPassword, password);
        }
    }
    
    public String getPassword() {
        return password;
    }

    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return calcSize();
    }
    
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return calcSize();
    }

    private Dimension calcSize() {
        Insets insets = getInsets();
        return new Dimension(
                (indicatorWidth + indicatorXSpacing) * maxColumns - indicatorXSpacing + insets.left + insets.right,
                (indicatorHeight + indicatorYSpacing) * maxRows - indicatorYSpacing + insets.top + insets.bottom);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(getForeground());
        Insets insets = getInsets();
        int x = insets.left;
        int startY = getHeight() - insets.bottom - indicatorHeight;
        for (Column column : columns) {
            int y = startY;
            g.setColor(getColor(column));
            for (int j = 0; j < column.getHeight(); j++) {
                g.fillRect(x, y, indicatorWidth, indicatorHeight);
                y -= (indicatorHeight + indicatorYSpacing);
            }
            x += (indicatorWidth + indicatorXSpacing);
        }
    }
    
    private Color getColor(Column column) {
        Color color = null;
        switch(column.getType()) {
            case DIGIT:
                color = digitColor;
                break;
            case UPPER_CASE:
                color = upperCaseColor;
                break;
            case LOWER_CASE:
                color = lowerCaseColor;
                break;
            case OTHER:
                color = otherColor;
                break;
            default:
                break;
        }
        if (color == null) {
            color = getForeground();
        }
        return color;
    }
    
    private void updateHeightsFromTarget() {
        boolean differ = false;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].adjustHeight(targetColumns[i].getHeight())) {
                differ = true;
            }
        }
        if (!differ) {
            stopTimer();
        } else {
            // PENDING: optimize repaint region
            repaint();
        }
    }

    private void update() {
        Column[] newColumns = calculateColumns();
        boolean differ = false;
        for (int i = 0; i < newColumns.length; i++) {
            columns[i].setType(newColumns[i].getType());
            if (columns[i].getHeight() != newColumns[i].getHeight()) {
                differ = true;
            }
        }
        if (animatesTransitions) {
            if (differ) {
                targetColumns = newColumns;
                startTimer();
            } else {
                stopTimer();
            }
        } else {
            columns = newColumns;
            stopTimer();
        }
        revalidate();
        repaint();
    }
    
    private void startTimer() {
        if (timer == null) {
            timer = new Timer(30, new ActionHandler());
            timer.setRepeats(true);
            timer.start();
        }
    }
    
    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private Column[] calculateColumns() {
        Column[] columns = new Column[maxColumns];
        String password = getPassword();
        if (password != null && password.length() > 0) {
            // Calculate the range
            Range lowerRange = new Range();
            Range upperRange = new Range();
            Range digitRange = new Range();
            Range otherRange = new Range();
            for (int i = 0; i < password.length(); i++) {
                char aChar = password.charAt(i);
                if (Character.isUpperCase(aChar)) {
                    upperRange.adjust(aChar);
                } else if (Character.isLowerCase(aChar)) {
                    lowerRange.adjust(aChar);
                } else if (Character.isDigit(aChar)) {
                    digitRange.adjust(aChar);
                } else {
                    otherRange.adjust(aChar);
                }
            }
            // Calculate the values in the range
            List<Column> values = new ArrayList<Column>(password.length());
            int max = maxRows - 2;
            for (int i = 0; i < password.length(); i++) {
                char aChar = password.charAt(i);
                Range range;
                Type type;
                if (Character.isUpperCase(aChar)) {
                    range = upperRange;
                    type = Type.UPPER_CASE;
                } else if (Character.isLowerCase(aChar)) {
                    range = lowerRange;
                    type = Type.LOWER_CASE;
                } else if (Character.isDigit(aChar)) {
                    range = digitRange;
                    type = Type.DIGIT;
                } else {
                    range = otherRange;
                    type = Type.OTHER;
                }
                int value = 2 + (int)(range.getPercent(aChar) * (float)max);
                values.add(new Column(type, value));
            }
            Collections.shuffle(values);
            for (int i = 0; i < Math.min(columns.length, values.size()); i++) {
                columns[i] = values.get(i);
            }
            for (int i = Math.min(columns.length, values.size()); i < columns.length; i++) {
                columns[i] = new Column();
            }
        } else {
            for (int i = 0; i < columns.length; i++) {
                columns[i] = new Column();
            }
        }
        return columns;
    }

    public void setAnimatesTransitions(boolean b) {
        animatesTransitions = b;
    }
    
    
    private class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateHeightsFromTarget();
        }
    }
    
    
    private static class Column {
        private int height;
        private Type type;
        
        public Column() {
            type = Type.NONE;
            height = 1;
        }
        
        public Column(Type type, int height) {
            this.height = height;
            this.type = type;
        }
        
        public boolean adjustHeight(int targetHeight) {
            if (targetHeight < height) {
                height--;
                return true;
            } else if (targetHeight > height) {
                height++;
                return true;
            }
            return false;
        }
        
        public void setType(Type type) {
            this.type = type;
        }
        
        public Type getType() {
            return type;
        }

        private int getHeight() {
            return height;
        }
    }


    private static class Range {
        private int min = -1;
        private int max = -1;
        
        public void adjust(int value) {
            if (min == -1) {
                min = max = value;
            } else if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
        }
        
        public float getPercent(int value) {
            if (min == max) {
                return 0;
            }
            return (float)(value - min) / (float)(max - min);
        }
        
        public String toString() {
            return "Range [" + min +"-" + max + "]";
        }
    }
}
