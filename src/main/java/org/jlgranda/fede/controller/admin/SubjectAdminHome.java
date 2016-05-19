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
package org.jlgranda.fede.controller.admin;

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
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.GroupHome;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.SubjectHome;
import org.jlgranda.fede.ui.model.LazySubjectDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.StringValidations;
import org.omnifaces.cdi.ViewScoped;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
@Named(value = "subjectAdminHome")
@ViewScoped
public class SubjectAdminHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 2914718473249636007L;

    Logger logger = LoggerFactory.getLogger(SubjectAdminHome.class);

    private Long subjectId;
    @Inject
    @LoggedIn
    private Subject subject;
    private Subject subjectEdit;

    @Inject
    private SettingHome settingHome;
    @Inject
    GroupHome groupHome;
    @Inject
    private PartitionManager partitionManager;
    IdentityManager identityManager = null;

    @Resource
    private UserTransaction userTransaction;
    @EJB
    private GroupService groupService;
    @EJB
    SubjectService subjectService;
    @EJB
    SettingService settingService;
    private List<org.jpapi.model.Group> groups;

    private LazySubjectDataModel lazyDataModel;

    @Inject
    private SubjectHome subjectHome;
    private String confirmarClave;
    private String clave;
    private boolean cambiarClave;

    public SubjectAdminHome() {
        this.groups = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }
        this.cambiarClave = false;
        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setOutcome("admin-subject");
        setSubjectEdit(subjectService.createInstance()); //Siempre listo para recibir la petición de creación
        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    @Override
    public List<org.jpapi.model.Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "admin", org.jpapi.model.Group.Type.LABEL);
        }

        return groups;
    }

    @Override
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

    public void mostrarFormularioCambiarClave() {
        this.cambiarClave = true;
//        RequestContext.getCurrentInstance().execute("PF('dlgCambiarClave').show()");
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
                
                //TODO leer desde configuración el url destino
                //redirectTo("/pages/admin/subject/profile.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
                redirectTo("/pages/admin/subject/profile_summary.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    public void save() {
        //Realizar signup
        if (!subjectEdit.isPersistent()) {
            subjectHome.processSignup(getSubjectEdit(), subject); //El propietario es el administrador actual
            addDefaultSuccessMessage();
        } else {
            //Solo actualizar
            subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
            addDefaultSuccessMessage();
        }
    }

    public void confirm(boolean force) {
        if (force) {
            getSubjectEdit().setConfirmed(false);
            subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
        }

        if (!getSubjectEdit().isConfirmed()) {
            subjectHome.sendConfirmation(getSubjectEdit());
        } else {
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.subject.confirmed"));
        }
    }

    /**
     * El método debe actualizar en picketlink, de otra manera no tiene efecto
     * el cambio de clave.
     */
    public void changePassword() {
        if (!StringValidations.isPassword(clave)) {
            addErrorMessage(I18nUtil.getMessages("passwordInvalidMsg"), I18nUtil.getMessages("passwordInvalidMsg"));
            return;
        }
        if (this.clave.equals(this.confirmarClave)) {
            try {
                identityManager = partitionManager.createIdentityManager();
                this.userTransaction.begin();
                Password password = new Password(this.clave);
                User user = BasicModel.getUser(identityManager, getSubjectEdit().getUsername());
                identityManager.update(user);
                identityManager.updateCredential(user, password);
                this.userTransaction.commit();
                String password_ = new BasicPasswordEncryptor().encryptPassword(new String(password.getValue()));
                subjectEdit.setPassword(password_);
                subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
                addDefaultSuccessMessage();
            } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            addErrorMessage(I18nUtil.getMessages("passwordsDontMatch"), I18nUtil.getMessages("passwordsDontMatch"));
        }
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

    

    public void mostrarAsignarGruposUsuarios() {
        try {
            List<Subject> subjects = new ArrayList<>();
            getSelectedBussinesEntities().stream().map((entity) -> (Subject) entity).forEach((s) -> {
                subjects.add(s);
            });

            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(I18nUtil.getMessages("subject.selected"), getSelectedBussinesEntities());
            redirectTo("/pages/admin/subject/subjects_group.jsf");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public String getConfirmarClave() {
        return confirmarClave;
    }

    public void setConfirmarClave(String confirmarClave) {
        this.confirmarClave = confirmarClave;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isCambiarClave() {
        return cambiarClave;
    }

    public void setCambiarClave(boolean cambiarClave) {
        this.cambiarClave = cambiarClave;
    }

}
