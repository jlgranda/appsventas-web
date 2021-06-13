/*
 * Copyright (C) 2021 kellypaulinc
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
import com.jlgranda.fede.ejb.RecordDetailTemplateService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import net.tecnopro.document.model.Tarea;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.RecordDetailTemplate;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.ui.model.LazyRecordTemplateDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class RecordTemplateHome extends FedeController implements Serializable {
    
    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(RecordTemplateHome.class);
    
    @Inject
    private SettingHome settingHome;
    
    @Inject
    private Subject subject;
    
    @EJB
    private RecordTemplateService recordTemplateService;
    
    @EJB
    private RecordDetailTemplateService recordDetailTemplateService;
    
    @Inject
    private OrganizationData organizationData;
    
    /**
     * El objeto Record para edición
     */
    private RecordTemplate recordTemplate;

    /**
     * RecordDetail para edición
     */
    private RecordDetailTemplate recordDetailTemplate;
    
    private RecordTemplate recordTemplateSelected;
    
    /**
     * Controla el comportamiento del controlador y pantalla
     */
    private LazyRecordTemplateDataModel lazyDataModel;
    
    private Long recordTemplateId;
    
    @PostConstruct
    private void init() {
        this.recordTemplate = recordTemplateService.createInstance();
        this.recordDetailTemplate = recordDetailTemplateService.createInstance();
        
    }

    public RecordDetailTemplate getRecordDetailTemplate() {
        return recordDetailTemplate;
    }

    public void setRecordDetailTemplate(RecordDetailTemplate recordDetailTemplate) {
        this.recordDetailTemplate = recordDetailTemplate;
    }

    public LazyRecordTemplateDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyRecordTemplateDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Long getRecordTemplateId() {
        return recordTemplateId;
    }

    public void setRecordTemplateId(Long recordTemplateId) {
        this.recordTemplateId = recordTemplateId;
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public RecordTemplate getRecordTemplate() {
        return recordTemplate;
    }

    public void setRecordTemplate(RecordTemplate recordTemplate) {
        this.recordTemplate = recordTemplate;
    }

    public RecordTemplate getRecordTemplateSelected() {
        return recordTemplateSelected;
    }

    public void setRecordTemplateSelected(RecordTemplate recordTemplateSelected) {
        this.recordTemplateSelected = recordTemplateSelected;
    }

    public boolean mostrarFormularioRecordTemplateEdicionValues(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_RECORDTEMPLATE_EDICION, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioEdicion(RecordTemplate recordTemplate) {
        
        return mostrarFormularioRecordTemplateEdicionValues(null);
    }

    public void closeFormularioRecordTemplateEdicion(Object data) {
        removeSessionParameter("recordtemplate");
        super.closeDialog(data);
    }
    
    public void onRowSelect(SelectEvent event) throws IOException {
        //Redireccionar a RIDE de objeto seleccionado
        if (event != null && event.getObject() != null) {
            //redirectTo("/pages/management/tarea/tarea.jsf?tareaId=" + ((BussinesEntity) event.getObject()).getId());
            RecordTemplate _recordTemplate = (RecordTemplate) event.getObject();
            redirectTo("/pages/accounting/admin/record_template.jsf?recordTemplateId=" + _recordTemplate.getId());
        }
    }
    
}
