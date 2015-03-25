/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.network;

import java.util.UUID;
import se.sics.kompics.Direct;

/**
 *
 * @author lkroll
 */
public class MessageNotify {
    
    public static Req create(Msg msg) {
        Req req = new Req(msg);
        return req;
    }
    
    public static class Req extends Direct.Request<Resp> {
        
        public final Msg msg;
        
        public Req(Msg msg) {
            this.msg = msg;
            this.setResponse(new Resp());
        }
        
        public UUID getMsgId() {
            return this.getResponse().msgId;
        }
        
        public void prepareResponse(long time, boolean success) {
            this.getResponse().setTime(time);
            this.getResponse().setSuccess(success);
        }
    }
    
    public static class Resp implements Direct.Response {
        
        private long time;
        public final UUID msgId;
        private boolean success = false;
        
        public Resp() {
            msgId = UUID.randomUUID();
        }
        
        void setTime(long time) {
            this.time = time;
        }
        
        public long getTime() {
            return time;
        }
        
        void setSuccess(boolean status) {
            this.success = status;
        }
        
        public boolean isSuccess() {
            return this.success;
        }
    }
}
