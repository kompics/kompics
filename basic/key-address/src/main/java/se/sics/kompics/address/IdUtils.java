/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.address;

import com.google.common.primitives.UnsignedBytes;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class IdUtils {
    public static void storeFormat(byte[] id, StringBuilder sb) {
        sb.append("0x");
        for (int i = 0; i < id.length; i++) {
            String bStr = UnsignedBytes.toString(id[i], 16);
            if (bStr.length() == 1) {
                sb.append('0');
            }
            sb.append(bStr.toUpperCase());
        }
    }
    
    public static String storeFormat(byte[] id) {
        StringBuilder sb = new StringBuilder();
        storeFormat(id, sb);
        return sb.toString();
    }
    
    public static byte[] parseStoreFormat(String str) {
        String[] bases = str.split("x");
        if ((bases.length == 2) 
                && (bases[0].equals("0")) 
                && (bases[1].length() > 0) 
                && (bases[1].length() % 2 == 0)) {
            String parseStr = bases[1];
            byte[] res = new byte[parseStr.length()/2];
            int i = 0;
            int j = 0;
            while (i < parseStr.length()) {
                String byteStr = parseStr.substring(i, i+2);
                res[j] = UnsignedBytes.parseUnsignedByte(byteStr, 16);
                i += 2;
                j++;
            }
            return res;
        } else {
            throw new NumberFormatException("'" + str + "' can not be converted to id type byte[]");
        }
    }
    
    public static void printFormat(byte[] id, StringBuilder sb) {
        for (int i = 0; i < id.length; i++) {
            String bStr = UnsignedBytes.toString(id[i], 16);
            if (bStr.length() == 1) {
                sb.append('0');
            }
            sb.append(bStr.toUpperCase());
            if (i + 1 < id.length) { // No space at the end
                sb.append(' ');
            }
        }
    }
    
    public static String printFormat(byte[] id) {
        StringBuilder sb = new StringBuilder();
        printFormat(id, sb);
        return sb.toString();
    }
}
