/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.wan.config;

import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author jdowling
 */
public interface SystemConfiguration {

    Properties set() throws IOException;

}
