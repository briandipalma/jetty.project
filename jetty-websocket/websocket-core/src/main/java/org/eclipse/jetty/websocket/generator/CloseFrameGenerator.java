package org.eclipse.jetty.websocket.generator;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.frames.BaseFrame;
import org.eclipse.jetty.websocket.util.CloseUtil;

public class CloseFrameGenerator extends FrameGenerator
{
    public CloseFrameGenerator(WebSocketPolicy policy)
    {
        super(policy);
    }

    @Override
    public void fillPayload(ByteBuffer buffer, BaseFrame close)
    {
        CloseUtil.assertValidPayload(close.getPayload());
        super.fillPayload(buffer,close);
    }
}
