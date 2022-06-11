package com.whatsapp.spark.kafka;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SendWhatsApp {
   public void sendmessage(String yourmessage) {
        Message message = Message.creator(
                new com.twilio.type.PhoneNumber("whatsapp:+919090155075"),
                new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                yourmessage)
                .create();

        System.out.println(message.getSid());
    }

    public void init() {
        String ACCOUNT_SID = "AC41097b109741763fb7d200b4bccf4ec8";
        String AUTH_TOKEN =  "86c3364c0780e50eb2b0a083aef97785";

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }
}
