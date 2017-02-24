/* 
 * Copyright 2012 Michael Pantazoglou
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
package org.geosde.compute.p2p;

/**
 * This interface is used for p2p message exchange.
 * 
 * @author Michael Pantazoglou
 */
public interface P2PConnection {

    /**
     * Sends the specified message to the node at the other end of this
     * connection.
     *
     * @param msg
     * @throws Exception
     */
    public void send(P2PMessage msg) throws Exception;

    /**
     * Receives a message from the node at the other end of this connection.
     *
     * @return the received message
     * @throws Exception
     */
    public P2PMessage receive() throws Exception;

    /**
     * Closes this connection.
     * 
     * @throws Exception
     */
    public void close() throws Exception;

}
