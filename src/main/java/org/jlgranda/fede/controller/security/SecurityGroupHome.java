/*
 * Copyright 2013 cesar.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jlgranda.fede.controller.security;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jlgranda.fede.controller.FedeController;
import org.omnifaces.cdi.ViewScoped;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.primefaces.event.SelectEvent;

@Named
@ViewScoped
public class SecurityGroupHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 7632987414391869389L;
    @Inject
    private PartitionManager partitionManager;
    @Resource
    private UserTransaction userTransaction;
    IdentityManager identityManager = null;
    private Group group;
    private String groupKey;

    @PostConstruct
    public void init() {
        group = createInstance();
        setOutcome("group-inbox");
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroup() {

        if (this.groupKey != null && group.getId() == null) {
            try {
                Group g = find();
                if (g != null) {
                    group = g;
                }
            } catch (IdentityException ex) {
                Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return group;
    }

    public void saveGroup() {
        identityManager = partitionManager.createIdentityManager();
        try {
            if (this.group.getId() != null) {
                this.userTransaction.begin();
                identityManager.update(group);
                this.addDefaultSuccessMessage();
                this.userTransaction.commit();
                redirectTo("/pages/admin/security/group/inbox.jsf");
            }
            this.userTransaction.begin();
            identityManager.add(group);
            this.userTransaction.commit();
            redirectTo("/pages/admin/security/group/inbox.jsf");

        } catch (IdentityManagementException |
                SecurityException | IllegalStateException e) {
            try {
                this.userTransaction.rollback();
            } catch (SystemException ignore) {
            }
            throw new RuntimeException("Could not create default security entities.", e);
        } catch (NotSupportedException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SystemException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RollbackException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        } catch (HeuristicMixedException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        } catch (HeuristicRollbackException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Group find() throws IdentityException {
        identityManager = partitionManager.createIdentityManager();
        Group group = BasicModel.getGroup(this.identityManager, this.groupKey);
        return group;
    }

    protected Group createInstance() {
        Group u = new Group("NEW GROUP");
        return u;
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        return null;
    }
}
