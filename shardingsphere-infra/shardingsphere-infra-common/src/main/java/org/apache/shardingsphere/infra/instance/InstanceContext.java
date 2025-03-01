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

package org.apache.shardingsphere.infra.instance;

import lombok.Getter;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.instance.definition.InstanceType;
import org.apache.shardingsphere.infra.instance.workerid.WorkerIdGenerator;
import org.apache.shardingsphere.infra.state.StateContext;
import org.apache.shardingsphere.infra.state.StateType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * Instance context.
 */
@Getter
public final class InstanceContext {
    
    private final ComputeNodeInstance instance;
    
    private final StateContext state = new StateContext();
    
    private final WorkerIdGenerator workerIdGenerator;
    
    private final ModeConfiguration modeConfiguration;
    
    private final Collection<ComputeNodeInstance> computeNodeInstances = new LinkedList<>();
    
    public InstanceContext(final ComputeNodeInstance instance, final WorkerIdGenerator workerIdGenerator, final ModeConfiguration modeConfiguration) {
        this.instance = instance;
        switchInstanceState(instance.getStatus());
        this.workerIdGenerator = workerIdGenerator;
        this.modeConfiguration = modeConfiguration;
    }
    
    /**
     * Update instance status.
     * 
     * @param status collection of status
     */
    public void updateInstanceStatus(final Collection<String> status) {
        instance.setStatus(status);
        switchInstanceState(status);
    }
    
    private void switchInstanceState(final Collection<String> status) {
        state.switchState(StateType.CIRCUIT_BREAK, null != status && status.contains(StateType.CIRCUIT_BREAK.name()));
    }
    
    /**
     * Update instance worker id.
     * 
     * @param workerId worker id
     */
    public void updateWorkerId(final Long workerId) {
        if (!Objects.equals(workerId, instance.getWorkerId())) {
            instance.setWorkerId(workerId);
        }
    }
    
    /**
     * Update instance label.
     * 
     * @param instanceId instance id
     * @param labels collection of label
     */
    public void updateLabel(final String instanceId, final Collection<String> labels) {
        if (instance.getInstanceDefinition().getInstanceId().getId().equals(instanceId)) {
            instance.setLabels(labels);
        }
        computeNodeInstances.stream().filter(each -> each.getInstanceDefinition().getInstanceId().getId().equals(instanceId)).forEach(each -> each.setLabels(labels));
    }
    
    /**
     * Get worker id.
     *
     * @return worker id
     */
    public long getWorkerId() {
        if (null == instance.getWorkerId()) {
            // TODO process generate failed
            Optional.of(workerIdGenerator.generate()).ifPresent(instance::setWorkerId);
        }
        return instance.getWorkerId();
    }
    
    /**
     * Add compute node instance.
     * 
     * @param instance compute node instance
     */
    public void addComputeNodeInstance(final ComputeNodeInstance instance) {
        computeNodeInstances.removeIf(each -> each.getInstanceDefinition().getInstanceId().getId().equalsIgnoreCase(instance.getInstanceDefinition().getInstanceId().getId()));
        computeNodeInstances.add(instance);
    }
    
    /**
     * Delete compute node instance.
     *
     * @param instance compute node instance
     */
    public void deleteComputeNodeInstance(final ComputeNodeInstance instance) {
        computeNodeInstances.removeIf(each -> each.getInstanceDefinition().getInstanceId().getId().equalsIgnoreCase(instance.getInstanceDefinition().getInstanceId().getId()));
    }
    
    /**
     * Get compute node instances by instance type and labels.
     *
     * @param instanceType instance type
     * @param labels collection of contained label
     * @return compute node instances
     */
    public Collection<ComputeNodeInstance> getComputeNodeInstances(final InstanceType instanceType, final Collection<String> labels) {
        Collection<ComputeNodeInstance> result = new ArrayList<>(computeNodeInstances.size());
        computeNodeInstances.forEach(each -> {
            if (each.getInstanceDefinition().getInstanceType() == instanceType && each.getLabels().stream().anyMatch(labels::contains)) {
                result.add(each);
            }
        });
        return result;
    }
}
