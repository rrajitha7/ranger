/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.elasticsearch.authorizer;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.services.elasticsearch.client.ElasticsearchResourceMgr;
import org.apache.ranger.services.elasticsearch.privilege.IndexPrivilegeUtils;
import org.elasticsearch.common.logging.ESLoggerFactory;

import com.google.common.collect.Sets;

public class RangerElasticsearchAuthorizer implements RangerElasticsearchAccessControl {

	private static final Logger LOG = ESLoggerFactory.getLogger(RangerElasticsearchAuthorizer.class);

	private static volatile RangerElasticsearchInnerPlugin elasticsearchPlugin = null;

	public RangerElasticsearchAuthorizer() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.RangerElasticsearchAuthorizer()");
		}

		this.init();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.RangerElasticsearchAuthorizer()");
		}
	}

	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.init()");
		}

		RangerElasticsearchInnerPlugin plugin = elasticsearchPlugin;

		if (plugin == null) {
			synchronized (RangerElasticsearchAuthorizer.class) {
				plugin = elasticsearchPlugin;

				if (plugin == null) {
					plugin = new RangerElasticsearchInnerPlugin();
					plugin.init();
					elasticsearchPlugin = plugin;
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.init()");
		}
	}

	@Override
	public boolean checkPermission(String user, List<String> groups, String index, String action,
			String clientIPAddress) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerElasticsearchAuthorizer.checkPermission( user=" + user + ", groups=" + groups
					+ ", index=" + index + ", action=" + action + ", clientIPAddress=" + clientIPAddress + ")");
		}

		boolean ret = false;

		if (elasticsearchPlugin != null) {

			String privilege = IndexPrivilegeUtils.getPrivilegeFromAction(action);
			String clusterName = elasticsearchPlugin.getClusterName();
			RangerElasticsearchAccessRequest request = new RangerElasticsearchAccessRequest(user, groups, index,
					privilege, clusterName, clientIPAddress);

			RangerAccessResult result = elasticsearchPlugin.isAccessAllowed(request);
			if (result != null && result.getIsAllowed()) {
				ret = true;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerElasticsearchAuthorizer.checkPermission(): result=" + ret);
		}

		return ret;
	}
}

class RangerElasticsearchInnerPlugin extends RangerBasePlugin {
	public RangerElasticsearchInnerPlugin() {
		super("elasticsearch", "elasticsearch");
	}

	@Override
	public void init() {
		super.init();

		RangerDefaultAuditHandler auditHandler = new RangerDefaultAuditHandler();

		super.setResultProcessor(auditHandler);
	}
}

class RangerElasticsearchResource extends RangerAccessResourceImpl {
	public RangerElasticsearchResource(String index) {
		if (StringUtils.isEmpty(index)) {
			index = "*";
		}
		setValue(ElasticsearchResourceMgr.INDEX, index);
	}
}

class RangerElasticsearchAccessRequest extends RangerAccessRequestImpl {
	public RangerElasticsearchAccessRequest(String user, List<String> groups, String index, String privilege,
			String clusterName, String clientIPAddress) {
		super.setUser(user);
		if (CollectionUtils.isNotEmpty(groups)) {
			super.setUserGroups(Sets.newHashSet(groups));
		}
		super.setResource(new RangerElasticsearchResource(index));
		super.setAccessType(privilege);
		super.setAction(privilege);
		super.setClusterName(clusterName);
		super.setClientIPAddress(clientIPAddress);
		super.setAccessTime(new Date());
	}
}
