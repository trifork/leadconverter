package com.trifork.conference.leadscanner;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import org.springframework.batch.item.ItemProcessor;

public class ScanToVcardItemProcessor implements ItemProcessor<Scan, VCard> {
    @Override
    public VCard process(Scan item) throws Exception {
        return Ezvcard.parse(item.getRawText()).first();
    }
}
