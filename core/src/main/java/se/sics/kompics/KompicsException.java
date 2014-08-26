/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics;

/**
 *
 * @author lkroll
 */
public class KompicsException extends RuntimeException {
    KompicsException(String msg) {
        this(msg, null);
    }
    KompicsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
