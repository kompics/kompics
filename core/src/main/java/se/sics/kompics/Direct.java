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
public class Direct {

    public static class Request<R extends Response> implements KompicsEvent {

        private Port origin;
        private R response;

        void setOrigin(Port origin) {
            if (this.origin == null) { // Only set origin once
                this.origin = origin;
            }
        }

        Port getOrigin() {
            return origin;
        }

        public void setResponse(R r) {
            this.response = r;
        }

        public R getResponse() {
            return response;
        }

        public boolean hasResponse() {
            return response != null;
        }
    }

    public static interface Response extends KompicsEvent {

    }
}
