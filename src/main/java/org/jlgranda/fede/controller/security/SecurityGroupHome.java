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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.ui.model.LazyGroupDataModel;
import org.jpapi.model.profile.Subject;
import org.omnifaces.cdi.ViewScoped;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.primefaces.event.SelectEvent;

@Named
@ViewScoped
public class SecurityGroupHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 7632987414391869389L;
    
    
    @Inject
    @LoggedIn
    private Subject subject;
    
    @Inject
    private SettingHome settingHome;
    
    @Inject
    private PartitionManager partitionManager;
    @Inject
    private IdentityManager identityManager;
    @Inject
    private RelationshipManager relationshipManager;
    @Resource
    private UserTransaction userTransaction;
    private Group group;
    private String groupKey;
    private LazyGroupDataModel lazyDataModel;
    
    @PersistenceContext
    private EntityManager entityManager;
    
     @Inject
    private SecurityGroupService securityGroupService;
     
    private List<Group> selectedGroups;
    
    @EJB
    private GroupService groupService;


    @PostConstruct
    public void init() {
        group = createInstance();
        setOutcome("admin-group");

        securityGroupService.setIdentityManager(identityManager);
        securityGroupService.setRelationshipManager(relationshipManager);
        securityGroupService.setPartitionManager(partitionManager);
        
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
        if (this.groupKey != null) {
            setGroup(securityGroupService.findByKey(groupKey));
        }
        return group;
    }

    public List<Group> getSelectedGroups() {
        return selectedGroups;
    }

    public void setSelectedGroups(List<Group> selectedGroups) {
        this.selectedGroups = selectedGroups;
    }

    public String saveGroup() {
        identityManager = partitionManager.createIdentityManager();
        try {
            if (this.group.getId() != null) {
                this.userTransaction.begin();
                identityManager.update(group);
                this.addDefaultSuccessMessage();
                this.userTransaction.commit();
                return "inboxGroup";
            } 
                this.userTransaction.begin();
                identityManager.add(group);
                this.userTransaction.commit();
                return "inboxGroup";

        } catch (IdentityManagementException |
                SecurityException | IllegalStateException e) {
            try {
                this.userTransaction.rollback();
            } catch (SystemException ignore) {
            }
            throw new RuntimeException("Could not create default security entities.", e);
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {
            Logger.getLogger(SecurityGroupHome.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    public LazyGroupDataModel getLazyDataModel() {

        filter();

        return lazyDataModel;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyGroupDataModel(securityGroupService);
        }

        lazyDataModel.setFilterValue(getKeyword());
    }

    public void setLazyDataModel(LazyGroupDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Group find() {
        identityManager = partitionManager.createIdentityManager();
        Group group = BasicModel.getGroup(this.identityManager, this.groupKey);
        return group;
    }

    protected Group createInstance() {
        return new Group(settingHome.getValue("app.admin.group.defaultname", "Nuevo grupo"));
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        return null;
    }

    @Override
    public List<org.jpapi.model.Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "security", "security"), org.jpapi.model.Group.Type.LABEL));
        }

        return this.groups;
    }
    
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                redirectTo("/pages/admin/security/group/group.jsf?groupKey=" + ((Group) event.getObject()).getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Group.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}