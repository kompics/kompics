/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.ui;

/**
 * Gives the strength of a password. This is meant to be illustrative only
 * and most definitely not used in any production settings.
 *
 * @author sky
 */
public final class PasswordStrengthAnalyzer extends Object {
    private static final PasswordStrengthAnalyzer INSTANCE = new PasswordStrengthAnalyzer();
    
    // 3 upper, 3 lower, 2 decimal or other
    // test for unique values
    public static float getInstanceStrength(String password) {
        return INSTANCE.getStrength(password);
    }
    
    PasswordStrengthAnalyzer() {
    }
    
    public float getStrength(String password) {
        // This is a rough implementation of that found at
        // http://www.lockdown.co.uk/?pg=combi&s=articles
        // This is by no means real. In particular a real analyzer would
        // make sure you don't have common words embedded in the string as well
        // as testing for a good distribution.
        if (password == null) {
            return 0;
        }
        int digitCount = 0;
        int lowerCount = 0;
        int upperCount = 0;
        int otherCount = 0;
        for (int i = 0; i < password.length(); i++) {
            char passwordChar = password.charAt(i);
            if (Character.isDigit(passwordChar)) {
                digitCount++;
            } else if (Character.isUpperCase(passwordChar)) {
                upperCount++;
            } else if (Character.isLowerCase(passwordChar)) {
                lowerCount++;
            } else {
                otherCount++;
            }
        }
        if (digitCount == 0 && upperCount == 0 && lowerCount == 0 &&
                otherCount == 0) {
            return 0;
        }
        int range = 0;
        if (digitCount > 0) {
            range += 10;
        }
        if (lowerCount > 0) {
            range += 26;
        }
        if (upperCount > 0) {
            range += 26;
        }
        if (otherCount > 0) {
            range += 36;
        }
        // 1 minute ~ 5.7e10 - 36
        // 1 hour ~ 3.5e12   - 42
        // 1 day ~ 7.5e13    - 47
        // 31 days ~ 2.7e15  - 52
        int bitCount = (32 - Integer.numberOfLeadingZeros(range)) * password.length();
        if (bitCount < 36) {
            return 0;
        }
        if (bitCount < 42) {
            return (float)(bitCount - 36) / 5f / 3f;
        }
        if (bitCount < 47) {
            return (float)(bitCount - 42) / 4f / 3f + 1f / 3f;
        }
        else if (bitCount < 52) {
            return (float)(bitCount - 47) / 4f / 3f + 2f / 3f;
        }
        return 1;
    }
}
