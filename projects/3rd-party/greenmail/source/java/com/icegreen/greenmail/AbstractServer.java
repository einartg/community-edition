/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 * 2012 - Alfresco Software, Ltd.
 * Alfresco Software has modified source of this file
 * The details of changes as svn diff can be found in svn at location root/projects/3rd-party/src
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Vector;
import javax.net.ssl.SSLServerSocket;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Feb 2, 2006
 */
public abstract class AbstractServer extends Service {
    protected InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected Vector handlers = null;
    protected Managers managers;
    protected ServerSetup setup;

    protected AbstractServer(ServerSetup setup, Managers managers) {
        try {
            this.setup = setup;
            bindTo = InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.managers = managers;
        handlers = new Vector();
    }

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret;
        if (setup.isSecure()) {
            ret = (SSLServerSocket) DummySSLServerSocketFactory.getDefault().createServerSocket(
                    setup.getPort(), 0, bindTo);
        } else {
            ret = new ServerSocket(setup.getPort(), 0, bindTo);
        }
        return ret;
    }

    public String getBindTo() {
        return bindTo.getHostAddress();
    }

    public int getPort() {
        return setup.getPort();
    }

    public String getProtocol() {
        return setup.getProtocol();
    }

    public ServerSetup getServerSetup() {
        return setup;
    }

    public void deregisterHandler(Object handler){
        this.handlers.remove(handler);
    }

    public String toString() {
        return null!=setup? setup.getProtocol()+':'+setup.getPort() : super.toString();
    }
}
