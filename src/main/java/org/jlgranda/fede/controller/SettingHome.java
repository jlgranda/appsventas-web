/*
 * Copyright (C) 2015 jlgranda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jlgranda.fede.controller;

import com.jlgranda.fede.ejb.SettingService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author jlgranda
 */
@ManagedBean
@ViewScoped
public class SettingHome extends FedeController implements Serializable {

    @Inject
    @LoggedIn
    private Subject subject;

    @EJB
    private SettingService settingService;

    private Setting setting;
   
    private Long settingId;
    private List<Setting> settings = new ArrayList<>();

    public void preRenderView() {
        this.buscar();
    }

    @PostConstruct
    public void init() {
        this.buscar();
        setSetting(settingService.createInstance());
    }

    public void crear() {
        settingService.createInstance();
    }

    public String getValue(String name, String defaultValue) {
        Setting s = settingService.findByName(name, subject);
        if (s == null) { //No existe configuración de usuario, tomar la configuración global, sino el valor por defecto
            return getGlobalValue(name, defaultValue);
        }
        return s.getValue();
    }

    public void buscar() {
        Setting settingBuscar = new Setting();
        settings = new ArrayList<>();
        //settingBuscar.setName(criterioBusqueda);
        List<Setting> settingsSistema = settingService.findByCriteriaOwnerNone(settingBuscar);
        Setting settingBuscar1 = new Setting();
        //settingBuscar1.setName(criterioBusqueda);
        settingBuscar1.setOwner(subject);
        List<Setting> settingsSubjects = settingService.findByCriteria(settingBuscar1);
        settings.addAll(settingsSistema);
        settings.addAll(settingsSubjects);
    }

    public void cancelar() {
        setSetting(settingService.createInstance());
    }

    public void save() {
        try {
            if (this.setting.getOwner() == null && this.setting.isPersistent()) {
                Setting settingnew = new Setting();
                settingnew.setName(getSetting().getName());
                settingnew.setValue(getSetting().getValue());
                settingnew.setDescription(getSetting().getDescription());
                setSetting(settingService.createInstance());
                getSetting().setUuid(java.util.UUID.randomUUID().toString());
                getSetting().setActive(Boolean.TRUE);
                getSetting().setCodeType(CodeType.SYSTEM);
                getSetting().setVersion(0);
                getSetting().setName(settingnew.getName());
                getSetting().setDescription(settingnew.getDescription());
                getSetting().setValue(settingnew.getValue());
                getSetting().setOwner(subject);
                settingService.save(setting);
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully"));
                return;
            }
            getSetting().setUuid(java.util.UUID.randomUUID().toString());
            getSetting().setActive(Boolean.TRUE);
            getSetting().setCodeType(CodeType.SYSTEM);
            getSetting().setVersion(0);
            getSetting().setOwner(subject);
            if (!this.setting.isPersistent()) {
                settingService.save(this.setting);
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully"));
                return;
            }
            settingService.save(getSetting().getId(),getSetting());
            addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully"));
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String param = (String) facesContext.getExternalContext().getRequestParameterMap().get(I18nUtil.getMessages("common.tipoGrabado"));
            if (param != null) {
                if (param.equalsIgnoreCase(I18nUtil.getMessages("common.tipoGrabado.save"))) {
                    settingService.createInstance();
                }
            }
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }

//    public void editar(Setting setting) {
//        setSetting(settingService.find(setting.getId()));
//    }

    public String getGlobalValue(String name, String defaultValue) {
        Setting s = settingService.findByName(name, null);
        if (s == null) {
            return defaultValue;
        }
        return s.getValue();
    }

    //<editor-fold defaultstate="collapsed" desc="SET Y GET">
    public List<Setting> getSettings() {
        return settings;
    }

    public void setSettings(List<Setting> settings) {
        this.settings = settings;
    }

    public Setting getSetting() {
        if (this.settingId!=null && !setting.isPersistent()){
            this.setting=settingService.find(settingId);
        }
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    //</editor-fold> 
    //<editor-fold defaultstate="collapsed" desc="METODOS DE FEDECONTROLLER">
    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
//</editor-fold>        
}
