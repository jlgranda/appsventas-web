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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SubjectService;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
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
import org.jlgranda.fede.controller.SettingHome;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.profile.Subject;
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
    @Inject
    SecurityGroupService securityGroupService;
    @Inject
    private SettingHome settingHome;
    IdentityManager identityManager = null;
    RelationshipManager relationshipManager = null;
    @Resource
    private UserTransaction userTransaction;
    @EJB
    SubjectService subjectService;
    @EJB
    private GroupService groupService;
    private List<BussinesEntity> selectedSubjects;
    private Group[] selectedGroups;
    private List<Group> grupos;
    private Subject subject;

    private Long subjectId;

    public SecurityGroupUserHome() {
    }

    @PostConstruct
    public void init() {
        setSubject(subjectService.createInstance());
        identityManager = partitionManager.createIdentityManager();
        securityGroupService.setIdentityManager(identityManager);

    }

    public void asignarGruposUsuarios() {

        identityManager = partitionManager.createIdentityManager();
        relationshipManager = partitionManager.createRelationshipManager();
//        this.subject = (Subject) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("profileSummary");
        User user = BasicModel.getUser(identityManager, this.subject.getUsername());

        for (Group g : selectedGroups) {
            try {
                this.userTransaction.begin();
                relationshipManager.add(new GroupMembership(user, g));
                this.userTransaction.commit();
            } catch (IdentityManagementException | NotSupportedException | SystemException |
                    HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException | RollbackException e) {
                java.util.logging.Logger.getLogger(SecurityGroupUserHome.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<org.jpapi.model.Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "admin", org.jpapi.model.Group.Type.LABEL);
        }

        return groups;
    }

    public List<BussinesEntity> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setSelectedSubjects(List<BussinesEntity> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }

    public Group[] getSelectedGroups() {
        return selectedGroups;
    }

    public void setSelectedGroups(Group[] selectedGroups) {
        this.selectedGroups = selectedGroups;
    }

    public List<Group> getGrupos() {
        try {
            this.grupos = securityGroupService.find();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(SecurityGroupUserHome.class.getName()).log(Level.SEVERE, null, e);
        }

        return grupos;
    }

    public boolean mostrarFormularioAsignarGruposUsuario() {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("profileSummary", getSubject());
        String width = settingHome.getValue(SettingNames.POPUP_SMALL_WIDTH, "400");
        String height = settingHome.getValue(SettingNames.POPUP_SMALL_HEIGHT, "240");
        super.openDialog(SettingNames.POPUP_SELECCIONAR_GRUPOS_USUARIO, width, height, true);
        return true;
    }

    public void setGrupos(List<Group> grupos) {
        this.grupos = grupos;
    }

    public Subject getSubject() {
        if (subjectId != null && !this.subject.isPersistent()) {
            this.subject = subjectService.find(subjectId);
        }
        return subject;
    }

    public void setSubject(Subject subjectEdit) {
        this.subject = subjectEdit;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

}
