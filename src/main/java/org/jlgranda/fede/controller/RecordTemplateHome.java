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
import com.jlgranda.fede.ejb.RecordTemplateService;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.ui.model.LazyRecordTemplateDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.Organization;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.component.selectonemenu.SelectOneMenu;
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

    @Inject
    private OrganizationData organizationData;

    /**
     * El objeto Record para edición
     */
    private RecordTemplate recordTemplate;

    private RecordTemplate recordTemplateSelected;

    /**
     * Controla el comportamiento del controlador y pantalla
     */
    private LazyRecordTemplateDataModel lazyDataModel;

    private Long recordTemplateId;
    
    private Organization organizationFuenteReglas;
    private List<RecordTemplate> selectedRecordTemplates;

    @PostConstruct
    private void init() {
        this.recordTemplate = recordTemplateService.createInstance();
        this.clear();
        this.setOutcome("record-templates");
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

    public List<RecordTemplate> getSelectedRecordTemplates() {
        return selectedRecordTemplates;
    }

    public void setSelectedRecordTemplates(List<RecordTemplate> selectedRecordTemplates) {
        this.selectedRecordTemplates = selectedRecordTemplates;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        return new ArrayList<>();
    }

    public RecordTemplate getRecordTemplate() {
        if (this.recordTemplateId != null && !this.recordTemplate.isPersistent()) {
            this.recordTemplate = recordTemplateService.find(recordTemplateId);
        }
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

    public Organization getOrganizationFuenteReglas() {
        return organizationFuenteReglas;
    }

    public void setOrganizationFuenteReglas(Organization organizationFuenteReglas) {
        this.organizationFuenteReglas = organizationFuenteReglas;
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
            RecordTemplate _recordTemplate = (RecordTemplate) event.getObject();
            redirectTo("/pages/accounting/admin/record_template.jsf?recordTemplateId=" + _recordTemplate.getId());
        }
    }
    
    public void clear(){
        this.filter();
    }

    public void save() {
        if (recordTemplate.isPersistent()) {
            recordTemplate.setLastUpdate(Dates.now());
        } else {
            recordTemplate.setAuthor(this.subject);
            recordTemplate.setOwner(this.subject);
            recordTemplate.setOrganization(this.organizationData.getOrganization());

        }
        recordTemplateService.save(recordTemplate.getId(), recordTemplate);
    }
    
    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        
        this.filter(this.organizationData.getOrganization());
    }
    
    private void filter(Organization organization) {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyRecordTemplateDataModel(recordTemplateService);
        }
        lazyDataModel.setOrganization(organization);
        lazyDataModel.setStart(this.getStart());
        lazyDataModel.setEnd(this.getEnd());
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

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public void organizationValueChange(javax.faces.event.AjaxBehaviorEvent event) throws IOException {
        
        this.recordTemplateSelected = null;
        SelectOneMenu x = (SelectOneMenu) event.getSource();
        this.organizationFuenteReglas = ((Organization) x.getValue());
        this.filter(this.organizationFuenteReglas); //Actualizar lista de reglas de negocio
    }
    
    public void importarRegla(){
        
        //simplemente copiar los valores
        this.recordTemplate.setCode(this.recordTemplateSelected.getCode());
        this.recordTemplate.setName(this.recordTemplateSelected.getName());
        this.recordTemplate.setRule(this.recordTemplateSelected.getRule());
    }
    
    public void importarReglas(){
        RecordTemplate instancia = null;
        for (RecordTemplate rt : this.getSelectedRecordTemplates()){
            
            instancia = recordTemplateService.createInstance();
            instancia.setCode(rt.getCode());
            instancia.setName(rt.getName());
            instancia.setRule(rt.getRule());
            instancia.setAuthor(this.subject);
            instancia.setOwner(this.subject);
            instancia.setOrganization(this.organizationData.getOrganization());
            
            recordTemplateService.save(instancia);
        }
        
        //Encerar pantalla
        this.selectedRecordTemplates.clear();
        this.organizationFuenteReglas = null;
        this.clear();
    }
    
    public void prepararImportarReglas(){
        this.organizationFuenteReglas = null;
        this.selectedRecordTemplates.clear();
    }
    
}
