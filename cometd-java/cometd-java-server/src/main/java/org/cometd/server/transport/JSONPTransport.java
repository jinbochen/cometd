/*
 * Copyright (c) 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cometd.server.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cometd.bayeux.server.ServerMessage;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.ServerSessionImpl;

public class JSONPTransport extends LongPollingTransport
{
    public final static String PREFIX = "long-polling.jsonp";
    public final static String NAME = "callback-polling";
    public final static String MIME_TYPE_OPTION = "mimeType";
    public final static String CALLBACK_PARAMETER_OPTION = "callbackParameter";

    private String _mimeType = "text/javascript;charset=UTF-8";
    private String _callbackParam = "jsonp";

    public JSONPTransport(BayeuxServerImpl bayeux)
    {
        super(bayeux, NAME);
        setOptionPrefix(PREFIX);
    }

    /**
     * @see org.cometd.server.transport.LongPollingTransport#isAlwaysFlushingAfterHandle()
     */
    @Override
    protected boolean isAlwaysFlushingAfterHandle()
    {
        return true;
    }

    /**
     * @see org.cometd.server.transport.JSONTransport#init()
     */
    @Override
    protected void init()
    {
        super.init();
        _callbackParam = getOption(CALLBACK_PARAMETER_OPTION, _callbackParam);
        _mimeType = getOption(MIME_TYPE_OPTION, _mimeType);
        // This transport must deliver only via /meta/connect
        setMetaConnectDeliveryOnly(true);
    }

    @Override
    public boolean accept(HttpServletRequest request)
    {
        return "GET".equals(request.getMethod()) && request.getParameter(getCallbackParameter()) != null;
    }

    @Override
    protected ServerMessage.Mutable[] parseMessages(HttpServletRequest request) throws IOException, ParseException
    {
        return super.parseMessages(request.getParameterValues(MESSAGE_PARAM));
    }

    public String getCallbackParameter()
    {
        return _callbackParam;
    }

    @Override
    protected PrintWriter writeMessage(HttpServletRequest request, HttpServletResponse response, PrintWriter writer, ServerSessionImpl session, ServerMessage message) throws IOException
    {
        if (writer == null)
        {
            response.setContentType(_mimeType);

            String callback = request.getParameter(_callbackParam);
            writer = response.getWriter();
            writer.append(callback);
            writer.append("([");
        }
        else
            writer.append(',');
        writer.append(message.getJSON());
        return writer;
    }

    @Override
    protected void finishWrite(PrintWriter writer, ServerSessionImpl session) throws IOException
    {
        writer.append("])");
        writer.close();
    }
}
