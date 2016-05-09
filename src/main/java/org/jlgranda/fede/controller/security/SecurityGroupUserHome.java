/*
 * Copyright (C) 2016 Jorge
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.controller.security;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jlgranda.fede.controller.FedeController;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.omnifaces.cdi.ViewScoped;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.GroupMembership;
import org.picketlink.idm.model.basic.User;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Jorge
 */
@Named(value = "securityGroupUserHome")
@ViewScoped
public class SecurityGroupUserHome extends FedeController implements Serializable {
    
    @Inject
    private PartitionManager partitionManager;
    IdentityManager identityManager = null;
    RelationshipManager relationshipManager = null;
    @Resource
    private UserTransaction userTransaction;
    private List<BussinesEntity> selectedSubjects;
    
    public SecurityGroupUserHome() {
    }
    
    @PostConstruct
    public void init() {
        selectedSubjects = (List<BussinesEntity>) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(I18nUtil.getMessages("subject.selected"));
//        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(I18nUtil.getMessages("subject.selected"));
    }
    
    public void asignarGruposUsuarios(Subject subject, List<Group> groups) throws RollbackException {
        
        identityManager = partitionManager.createIdentityManager();
        relationshipManager = partitionManager.createRelationshipManager();
        User user = BasicModel.getUser(identityManager, subject.getUsername());
        for (Group g : groups) {
            try {
                this.userTransaction.begin();
                relationshipManager.add(new GroupMembership(user, g));
                this.userTransaction.commit();
            } catch (IdentityManagementException | NotSupportedException | SystemException |
                    HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
                java.util.logging.Logger.getLogger(SecurityGroupUserHome.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public List<org.jpapi.model.Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<BussinesEntity> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setSelectedSubjects(List<BussinesEntity> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }
    
 
}
