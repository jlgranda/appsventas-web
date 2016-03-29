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
package org.jlgranda.fede.controller;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SettingService;
import com.jlgranda.fede.ejb.SubjectService;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.ui.model.LazySubjectDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.Strings;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import static org.picketlink.idm.model.basic.BasicModel.addToGroup;
import static org.picketlink.idm.model.basic.BasicModel.grantGroupRole;
import static org.picketlink.idm.model.basic.BasicModel.grantRole;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Jorge
 */
@Named(value = "subjectAdminHome")
@RequestScoped
public class SubjectAdminHome extends FedeController implements Serializable {

    private Long subjectId;
    @Inject
    @LoggedIn
    private Subject subject;
    private Subject subjectEdit;
    @Inject
    private SettingHome settingHome;
    @Inject
    private PartitionManager partitionManager;
    @Inject
    GroupHome groupHome;
    IdentityManager identityManager = null;
    @EJB
    private GroupService groupService;
    @EJB
    SubjectService subjectService;
    @EJB
    SettingService settingService;
    @Resource
    private UserTransaction userTransaction;
    private List<org.jpapi.model.Group> groups = new ArrayList<>();
    private LazySubjectDataModel lazyDataModel;

    public SubjectAdminHome() {
    }

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }

        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setOutcome("inboxInstanciaProceso");

        setSubjectEdit(subjectService.createInstance()); //Siempre listo para recibir la respuesta del proceso

        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public List<org.jpapi.model.Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "documents", org.jpapi.model.Group.Type.LABEL);
        }

        return groups;
    }

    public void setGroups(List<org.jpapi.model.Group> groups) {
        this.groups = groups;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazySubjectDataModel(subjectService);
        }
        lazyDataModel.setOwner(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());

        if (getKeyword() != null && getKeyword().startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setTags(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyDataModel.setTags(getTags());
            lazyDataModel.setFilterValue(getKeyword());
        }
    }

    public LazySubjectDataModel getLazyDataModel() {
        filter();

        return lazyDataModel;
    }

    public void setLazyDataModel(LazySubjectDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                redirectTo("/pages/admin/profile.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SubjectAdminHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
    }

    @Override
    public Group getDefaultGroup() {
        if (this.defaultGroup == null) {
            return groupService.findByCode(settingHome.getValue(SettingNames.DEFAULT_INVOICES_GROUP_NAME, "fede"));
        }
        return this.defaultGroup;
    }

    public void save(Subject subject) {
        if (!subjectEdit.isPersistent()) {
            identityManager = partitionManager.createIdentityManager();
            try {

                //Prepare password
                Password password = new Password(getSubjectEdit().getPassword());
                //separar nombres
                List<String> names = Strings.splitNamesAt(getSubjectEdit().getFirstname());

                if (names.size() > 1) {
                    getSubjectEdit().setFirstname(names.get(0));
                    getSubjectEdit().setSurname(names.get(1));
                }
                getSubjectEdit().setUsername(getSubjectEdit().getEmail());

                this.userTransaction.begin();
                User user = new User(getSubjectEdit().getUsername());
                user.setFirstName(getSubjectEdit().getFirstname());
                user.setLastName(getSubjectEdit().getSurname());
                user.setEmail(getSubjectEdit().getEmail());
                user.setCreatedDate(Dates.now());
                identityManager.add(user);

                identityManager.updateCredential(user, password);

                // Create application role "superuser"
                Role superuser = BasicModel.getRole(identityManager, "superuser");

                org.picketlink.idm.model.basic.Group group = BasicModel.getGroup(identityManager, "fede");

                RelationshipManager relationshipManager = partitionManager.createRelationshipManager();
                // Make john a member of the "sales" group
                addToGroup(relationshipManager, user, group);
                // Make mary a manager of the "sales" group
                grantGroupRole(relationshipManager, user, superuser, group);
                // Grant the "superuser" application role to jane
                grantRole(relationshipManager, user, superuser);

                this.userTransaction.commit();

                //Conectar con el user auth
                String passwrod_ = new BasicPasswordEncryptor().encryptPassword(new String(password.getValue()));
                getSubjectEdit().setUsername(getSubjectEdit().getEmail());
                getSubjectEdit().setCodeType(CodeType.CEDULA);
                getSubjectEdit().setPassword(passwrod_);
                getSubjectEdit().setUsernameConfirmed(true);

                //Set fede email
                getSubjectEdit().setFedeEmail(getSubjectEdit().getCode().concat("@").concat(settingService.findByName("mail.imap.host").getValue()));
                getSubjectEdit().setFedeEmailPassword(passwrod_);

                //Finalmente crear en fede
                getSubjectEdit().setUuid(user.getId());
                getSubjectEdit().setOwner(subject);
                getSubjectEdit().setSubjectType(Subject.Type.NATURAL);
                subjectService.save(getSubjectEdit());

                //Crear grupos por defecto para el subject
//                groupHome.createDefaultGroups(getSubjectEdit());
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
            } catch (NotSupportedException | SystemException | IdentityManagementException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
                try {
                    this.userTransaction.rollback();
                } catch (SystemException ignore) {
                }
                throw new RuntimeException("Could not create default security entities.", e);
            }
            return;
        }
        getSubjectEdit().setOwner(subject);
        subjectService.save(subject.getId(), subject);
        addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
    }

    public Subject getSubjectEdit() {
        if (subjectId != null && !this.subjectEdit.isPersistent()) {
            this.subjectEdit = subjectService.find(subjectId);
        }
        return subjectEdit;
    }

    public void setSubjectEdit(Subject subjectEdit) {
        this.subjectEdit = subjectEdit;
    }

    public void applySelectedGroups() {
        String status = "";
        Group group = null;
        Set<String> addedGroups = new LinkedHashSet<>();
        for (BussinesEntity fe : getSelectedBussinesEntities()) {
            for (String key : selectedTriStateGroups.keySet()) {
                group = findGroup(key);
                status = selectedTriStateGroups.get(key);
                if ("0".equalsIgnoreCase(status)) {
                    if (fe.containsGroup(key)) {
                        fe.remove(group);
                    }
                } else if ("1".equalsIgnoreCase(status)) {
                    if (!fe.containsGroup(key)) {
                        fe.add(group);
                        addedGroups.add(group.getName());
                    }
                } else if ("2".equalsIgnoreCase(status)) {
                    if (!fe.containsGroup(key)) {
                        fe.add(group);
                        addedGroups.add(group.getName());
                    }
                }
            }

            subjectService.save(fe.getId(), (Subject) fe);
        }

        this.addSuccessMessage("Las facturas se agregaron a " + Lists.toString(addedGroups), "");
    }

    private Group findGroup(String key) {
        for (Group g : getGroups()) {
            if (key.equalsIgnoreCase(g.getCode())) {
                return g;
            }
        }
        return new Group("null", "null");
    }
}
