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

import java.io.Serializable;
import java.net.URI;

/**
 * Defines a convenient abstraction of p2p endpoints.
 * 
 * @author Michael Pantazoglou
 */
public class P2PEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

	/**
     * The underlying address of this endpoint.
     */
    protected URI address;
    
    public P2PEndpoint() {
    	
    }
    
    public final void setAddress(URI a) {
    	address = a;
    }

    public final URI getAddress() {
        return address;
    }

    @Override
    public String toString() {
    	return address.toString();
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof P2PEndpoint) {
    		P2PEndpoint oEndpoint = (P2PEndpoint) o;
    		URI oAddress = oEndpoint.getAddress();
    		if (oAddress == null) {
    			return address==null;
    		} else {
    			return oAddress.equals(address);
    		}
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return address.hashCode();
    }
}
