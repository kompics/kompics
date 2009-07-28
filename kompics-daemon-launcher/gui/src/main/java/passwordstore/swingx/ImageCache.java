/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.swingx;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Provides a cache for scaled images.
 */
public class ImageCache {
    private static final ImageCache INSTANCE = new ImageCache();
    private final Map<CacheEntry,SoftReference<Image>> cache;
    
    /**
     * Returns a global instance of the ImageCache.
     *
     * @return shared instance of the cache
     */
    public static ImageCache getInstance() {
        return INSTANCE;
    }
    
    ImageCache() {
        // PENDING: this could use a slightly better caching scheme than
        // just soft refs, but it's a start
        cache = new HashMap<CacheEntry,SoftReference<Image>>();
    }

    /**
     * Returns a scaled images of the specified size.
     *
     * @param c the Component the image is displayed in
     * @param image the Image to scale
     * @param w the width
     * @param h the height
     * @return an image of the specified size
     */
    public Image getImage(Component c, Image image, int w, int h) {
        CacheEntry entry = new CacheEntry(image, w, h);
        SoftReference<Image> ref = cache.get(entry);
        Image cachedImage;
        if (ref == null || (cachedImage = ref.get()) == null) {
            cachedImage = createImage(c, image, w, h);
            cache.put(entry, new SoftReference<Image>(cachedImage));
        }
        return cachedImage;
    }
    
    /**
     * Returns a scaled images of the specified size.
     *
     * @param c the Component the image is displayed in
     * @param path path to obtain the image
     * @param w the width
     * @param h the height
     * @return an image of the specified size
     */
    public Image getImage(Component c, URI path, int w, int h) {
        CacheEntry entry = new CacheEntry(path, w, h);
        SoftReference<Image> ref = cache.get(entry);
        Image cachedImage;
        if (ref == null || (cachedImage = ref.get()) == null) {
            ImageIcon ii;
            try {
                ii = new ImageIcon(path.toURL());
                if (w == 0 || h == 0) {
                    cachedImage = ii.getImage();
                } else {
                    cachedImage = createImage(c, ii.getImage(), w, h);
                }
                cache.put(entry, new SoftReference<Image>(cachedImage));
            } catch (MalformedURLException ex) {
                cachedImage = null;
            }
        }
        return cachedImage;
    }
    
    private static Image createImage(Component c, Image image, int w, int h) {
        int iw = image.getWidth(null);
        int ih = image.getHeight(null);
        if (iw > 0 && ih > 0) {
            float aspectRatio = (float)iw /(float)ih;
            int targetWidth;
            int targetHeight;
            if (iw > ih) {
                targetWidth = w;
                targetHeight = (int)(targetWidth / aspectRatio);
                if (targetHeight > h) {
                    targetHeight = h;
                    targetWidth = (int)(aspectRatio * targetHeight);
                }
            } else {
                targetHeight = h;
                targetWidth = (int)(aspectRatio * targetHeight);
                if (targetWidth > w) {
                    targetWidth = w;
                    targetHeight = (int)(targetWidth / aspectRatio);
                }
            }
            if (targetWidth != iw || targetHeight != iw) {
                Image cachedImage;
                GraphicsConfiguration gc;
                if (c != null && (gc = c.getGraphicsConfiguration()) != null) {
                    cachedImage = gc.createCompatibleImage(targetWidth,
                            targetHeight, Transparency.TRANSLUCENT);
                } else {
                    cachedImage = new BufferedImage(targetWidth,
                            targetHeight, BufferedImage.TYPE_INT_ARGB);
                }
                Graphics imageG = cachedImage.getGraphics();
                if (imageG instanceof Graphics2D) {
                    ((Graphics2D)imageG).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }
                imageG.drawImage(image, 0, 0, targetWidth, targetHeight,
                        0, 0, iw, ih, null);
                imageG.dispose();
                return cachedImage;
            }
            return image;
        }
        return null;
    }
    
    
    private static final class CacheEntry {
        private final Object source;
        private final int width;
        private final int height;
        
        public CacheEntry(Object source, int width, int height) {
            if (source == null) {
                throw new IllegalArgumentException();
            }
            this.source = source;
            this.width = width;
            this.height = height;
        }

        public int hashCode() {
            int hash = 17;
            hash = 17 + 37 * source.hashCode();
            hash = hash + 37 * width;
            hash = hash + 37 * height;
            return hash;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof CacheEntry) {
                CacheEntry ce = (CacheEntry)obj;
                return (ce.width == width &&
                        ce.height == height &&
                        ce.source.equals(source));
            }
            return false;
        }
    }
}
