package com.damir.rma2_mateljic_damir;

public class GuidGenerator {

    public static String  generateGuid() {
        StringBuilder guid = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int randomDigit = (int) (Math.random() * 16);
            guid.append(Integer.toHexString(randomDigit));
        }
        return guid.toString();
    }
}
