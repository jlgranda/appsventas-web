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
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
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
import org.jpapi.util.Lists;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Jorge
 */
@ManagedBean(name= "subjectAdminHome")
@ViewScoped
public class SubjectAdminHome extends FedeController implements Serializable {

    private Long subjectId;
    @Inject
    @LoggedIn
    private Subject subject;
    private Subject subjectEdit;
    @Inject
    private SettingHome settingHome;
    @Inject
    GroupHome groupHome;
    IdentityManager identityManager = null;
    @EJB
    private GroupService groupService;
    @EJB
    SubjectService subjectService;
    @EJB
    SettingService settingService;
    private List<org.jpapi.model.Group> groups = new ArrayList<>();

    private LazySubjectDataModel lazyDataModel;

    @Inject
    private SubjectHome subjectHome;
    private String confirmarClave;

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

    public boolean mostrarFormularioCambiarClave() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(SettingNames.POPUP_FORMULARIO_CAMBIAR_CLAVE, width, height, true);
        return true;
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
                redirectTo("/pages/admin/subject/profile.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
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

    public void changePassword() {
        if (subjectEdit.getPassword().equals(this.confirmarClave)) {
            subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
            addDefaultSuccessMessage();
        } else {
            addWarningMessage("La clave no coinciden", "La clave no coinciden");
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

    public String getConfirmarClave() {
        return confirmarClave;
    }

    public void setConfirmarClave(String confirmarClave) {
        this.confirmarClave = confirmarClave;
    }

}
