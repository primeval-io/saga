package io.primeval.saga.ninio.internal;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.davfx.ninio.core.SendCallback;

public final class NinioNoOpSendCallback implements SendCallback {
    public static final Logger LOGGER = LoggerFactory.getLogger(NinioNoOpSendCallback.class);
    
    public static final NinioNoOpSendCallback INSTANCE = new NinioNoOpSendCallback();

    @Override
    public void failed(IOException e) {
        LOGGER.error("Failed to send reply", e);
    }

    @Override
    public void sent() {

    }

}
