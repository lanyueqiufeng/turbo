package com.didiglobal.turbo.engine.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

public final class StrongUuidGenerator implements IdGenerator {

    private static volatile TimeBasedGenerator timeBasedGenerator;

    public StrongUuidGenerator() {
        initGenerator();
    }

    private void initGenerator() {
        if (timeBasedGenerator == null) {
            synchronized (StrongUuidGenerator.class) {
                if (timeBasedGenerator == null) {
                    timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
                }
            }
        }
    }

    // public String getNextId() {
    //     return timeBasedGenerator.generate().toString();
    // }

    public String getNextId() {
        return IdWorker.getIdStr();
    }

}
