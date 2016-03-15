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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
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

    private List<Setting> overwritableSettings = new ArrayList<>();

    @PostConstruct
    public void init() {
        setSetting(settingService.createInstance());
    }

    public void crear() {
        setSetting(settingService.createInstance());
        getSetting().setOwner(subject);
        getSetting().setCodeType(CodeType.SUBJECT);
        getSetting().setOverwritable(false);
        getSetting().setUuid(java.util.UUID.randomUUID().toString());
        getSetting().setActive(Boolean.TRUE);
    }

    public String getValue(String name, String defaultValue) {
        Setting s = settingService.findByName(name, subject);
        if (s == null) { //No existe configuración de usuario, tomar la configuración global, sino el valor por defecto
            return getGlobalValue(name, defaultValue);
        }
        return s.getValue();
    }

    /**
     * Obtener todas las configuraciones del sistema que son suceptibles de ser
     * sobreescritar para el usuario actual
     */
    protected List<Setting> findSettingsForOverwrite() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("owner", null);
        filters.put("codeType", CodeType.SYSTEM); ///propiedades del sistema
        filters.put("overwritable", true);
        QueryData<Setting> queryData = settingService.find(-1, -1, "label", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }
//    /**
//     * Busca las configuraciones del sistema 
//     */
//    public void buscar() {
//        Map<String, Object> filters = new HashMap<>();
//        filters.put("owner", subject);
//        settingService.findByNamedQuery(outcome);
//        QueryData<Setting> queryData = settingService.find(-1, -1, "label", QuerySortOrder.ASC, filters);
//        settings.addAll(queryData.getResult());
//    }

    public void cancelar() {
        setSetting(settingService.createInstance());
    }

    public void save() {
        try {
            List<Setting> settingsbyNameAndOwner = settingService.findByNamedQuery("Setting.findByNameAndOwner", this.setting.getName(), subject);
            Setting settingByNameAndOwner = !settingsbyNameAndOwner.isEmpty() ? settingsbyNameAndOwner.get(0) : null;
            if (settingByNameAndOwner == null) {
                setting = newSetting(settingByNameAndOwner);
            }
            if (!this.setting.isPersistent()) {
                settingService.save(this.setting);
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully"));
                return;
            }
            settingService.save(getSetting().getId(), getSetting());
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
        if (settingId != null && !this.setting.isPersistent()) {
            this.setting = settingService.find(settingId);
        }
        return setting;
    }

    private Setting newSetting(Setting settingByNameAndOwner) {
        settingByNameAndOwner = settingService.createInstance();
        settingByNameAndOwner.setOwner(subject);
        settingByNameAndOwner.setCodeType(CodeType.SUBJECT);
        settingByNameAndOwner.setOverwritable(false);
        settingByNameAndOwner.setUuid(java.util.UUID.randomUUID().toString());
        settingByNameAndOwner.setActive(Boolean.TRUE);
        settingByNameAndOwner.setName(setting.getName());
        settingByNameAndOwner.setValue(setting.getValue());
        settingByNameAndOwner.setLabel(setting.getLabel());
        settingByNameAndOwner.setDescription(setting.getDescription());
        return settingByNameAndOwner;
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

    public List<Setting> getOverwritableSettings() {
        if (overwritableSettings.isEmpty()) {
            setOverwritableSettings(findSettingsForOverwrite());
        }
        return overwritableSettings;
    }

    public void setOverwritableSettings(List<Setting> overwritableSettings) {
        this.overwritableSettings = overwritableSettings;
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
