/*
 * Copyright 2018-present Open Networking Foundation
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
package DHCPRouting.app;

import org.onosproject.net.ConnectPoint;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * DHCP Routing Config class
 */
public class DhcpRoutingConfig extends Config<ApplicationId> {
	
	public static final String DEVICE_CONNECTPOINT = "deviceConnectPoint";

	@Override
	public boolean isValid() {
		return hasOnlyFields(DEVICE_CONNECTPOINT) &&
				isConnectPoint(DEVICE_CONNECTPOINT, MANDATORY);
	}

	/**
	 * Returns the device connect point
	 * 
	 * @return connect point or null if not set
	 */
	public ConnectPoint devicePoint() {
		String connectPoint = get(DEVICE_CONNECTPOINT, null);
		return connectPoint != null ? ConnectPoint.deviceConnectPoint(connectPoint) : null;
	}

	/**
	 * Sets the device connect point
	 *
	 * @param point new connect point; null to clear
	 * @return self
	 */
	public BasicElementConfig devicePoint(String point) {
		return (BasicElementConfig) setOrClear(DEVICE_CONNECTPOINT, point);
	}
}
