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
package org.geosde.compute.hypercube.services;

import org.geosde.compute.hypercube.HypercubeService;
import org.geosde.compute.p2p.P2PRequest;
import org.geosde.compute.p2p.P2PResponse;

/**
 * This service is used to set the provider node's position and cover map 
 * vectors.
 * 
 * @author Michael Pantazoglou
 *
 */
public class SetPositionAndCoverMapService extends HypercubeService {
	/**
	 * The request that triggered this service.
	 */
	private SetPositionAndCoverMapRequest request;
	
	/**
	 * Constructor.
	 */
	public SetPositionAndCoverMapService() {
		super();
	}

	@Override
	public void setRequest(P2PRequest req) {
		request = (SetPositionAndCoverMapRequest) req;
	}

	@Override
	public void execute() {
		me.setPositionVector(request.getPositionVector());
		me.setCoverMapVector(request.getCoverMapVector());
	}

	@Override
	public boolean isRequestResponse() {
		return false;
	}

	@Override
	public P2PResponse getResponse() {
		return null;
	}

}
