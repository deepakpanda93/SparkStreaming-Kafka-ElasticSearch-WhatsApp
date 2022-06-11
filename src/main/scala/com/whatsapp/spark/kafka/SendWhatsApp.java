package com.whatsapp.spark.kafka;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SendWhatsApp {
   public void sendmessage(String yourmessage) {
        Message message = Message.creator(
                new com.twilio.type.PhoneNumber("whatsapp:+YOUR_WHATSAPP_PHONE_NUMBER"),
                new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                yourmessage)
                .create();

        System.out.println(message.getSid());
    }

    public void init() {
        String ACCOUNT_SID = "TWILIO_ACCOUNT_SID";
        String AUTH_TOKEN =  "TWILIO_AUTH_TOKEN";

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }
}
