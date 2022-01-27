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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class SettingHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 3482094772845122813L;

    private Subject subject;

    @EJB
    private SettingService settingService;

    private Setting setting;

    private Long settingId;

    private String settingName;

    private List<Setting> settings = new ArrayList<>();

    private List<Setting> overwritableSettings = new ArrayList<>();

    private Map<String, String> cache = new HashMap<>();
    
    @Inject
    private OrganizationData organizationData;

    @PostConstruct
    public void init() {
        setOutcome("");
        crear();
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
        //Buscar en cache
        if (cache.containsKey(name)) {
            //logger.info("La propiedad {} se recupera desde cache.", name);
            return cache.get(name);
        }

        Setting s = settingService.findByNameAndOrganization(name, subject, this.organizationData.getOrganization().getId());
        if (s == null) { //No existe configuración de usuario, tomar la configuración global, sino el valor por defecto
            String value = getGlobalValue(name, defaultValue);
            cache.put(name, value);
            return value;
        }

        cache.put(name, s.getValue());
        return s.getValue();
    }

    /**
     * Obtener todas las configuraciones del sistema que son suceptibles de ser
     * sobreescritar para el usuario actual
     *
     * @return la lista de propiedades sobreescribibles
     */
    protected List<Setting> findSettingsForOverwrite() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("owner", null);
        filters.put("codeType", CodeType.SYSTEM); ///propiedades del sistema
        filters.put("overwritable", true);
        QueryData<Setting> queryData = settingService.find(-1, -1, "label", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }
    
    /**
     * Obtener todas las configuraciones del usuario
     *
     * @return la lista de propiedades sobreescribibles
     */
    protected List<Setting> findMySettings() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("owner", subject);
        filters.put("codeType", CodeType.SUBJECT); ///propiedades del sistema
        QueryData<Setting> queryData = settingService.find(-1, -1, "label", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }
    
    /**
     * Obtener todas las configuraciones del usuario con raíz
     *
     * @return la lista de propiedades sobreescribibles
     */
    public List<Setting> findSettings(String path) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("name", path);
        filters.put("owner", null);
        filters.put("codeType", CodeType.SYSTEM); ///propiedades del sistema
        QueryData<Setting> queryData = settingService.find(-1, -1, "label", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }

    public void cancelar() {
        setSetting(null);
    }

    public void save() {
        try {
            settingService.save(getSetting().getId(), getSetting());
            
            //Actualizar el cache
            this.cache.put(getSetting().getName(), getSetting().getValue());
            
            //vaciar objeto en edición
            setSetting(settingService.createInstance());
            
            addDefaultSuccessMessage();

        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }

    public void restaurar() {
        if (this.setting.isPersistent()) {
            List<Setting> settingsByName = settingService.findByNamedQuery("Setting.findByName", getSetting().getName());
            Setting settingByName = !settingsByName.isEmpty() ? settingsByName.get(0) : null;
            if (settingByName != null) {
                getSetting().setValue(settingByName.getValue());
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.setting.action.restaurar"));
            }
        }
    }

    /**
     * Retorna la configuración global para la organización
     * @param name
     * @param defaultValue
     * @return 
     */
    public String getGlobalValue(String name, String defaultValue) {
        Setting s = settingService.findByNameAndOrganization(name, subject, this.organizationData.getOrganization().getId());
        if (s == null) {
            //logger.info("SettingHome: La propiedad {} no esta definido. Se usará el valor {}", name, defaultValue);
            return defaultValue;
        }
        return s.getValue();
    }
    
    protected List<Setting> findByCodeType(CodeType codeType) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("codeType", codeType);
        QueryData<Setting> queryData = settingService.find(-1, -1, "category,value", QuerySortOrder.DESC, filters);
        return queryData.getResult();
    }
    
    //<editor-fold defaultstate="collapsed" desc="SET Y GET">
    public List<Setting> getSettings() {
        return settings;
    }

    public void setSettings(List<Setting> settings) {
        this.settings = settings;
    }

    public Setting getSetting() {
        if (!Strings.isNullOrEmpty(settingName) && this.setting == null) {
            //Cargar objeto setting por nombre
            Setting test = settingService.findByNameAndOrganization(settingName, subject, this.organizationData.getOrganization().getId());
            if (test != null) {
                //El objeto configuración de usuario
                this.setting = test;
            } else {
                //La configuración de la organización
                test = settingService.findByNameAndOrganization(settingName, null, this.organizationData.getOrganization().getId());
                if (test != null) {
                    //El objeto configuración de usuario
                    this.setting = buildSettingFrom(test);
                } else {
                    //Se toma la configuración de referencia del sistema
                    this.setting = buildSettingFrom(settingService.findByNameAndOrganization(settingName, null, null));
                }
            }
        } else if (settingId != null && this.setting == null) {
            //Cargar objeto setting por id, carga el objeto directamente.
            this.setting = settingService.find(settingId);
        }
        return this.setting;
    }

    private Setting buildSettingFrom(Setting src) {
        Setting dest = settingService.createInstance();
        dest.setOwner(subject);
        dest.setCodeType(CodeType.SUBJECT);
        dest.setOverwritable(false);
        dest.setUuid(java.util.UUID.randomUUID().toString());
        dest.setActive(Boolean.TRUE);
        dest.setName(src.getName());
        dest.setValue(src.getValue());
        dest.setLabel(src.getLabel());
        dest.setDescription(src.getDescription());
        return dest;
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

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        if (!settingName.equalsIgnoreCase(this.settingName)) {
            setSetting(null);
        }
        this.settingName = settingName;
    }

    public List<Setting> getOverwritableSettings() {
        if (overwritableSettings.isEmpty()) {
            addOverwritableSettings(findMySettings());
            addOverwritableSettings(findSettingsForOverwrite());
            Collections.sort(this.overwritableSettings);
        }
        return overwritableSettings;
    }

    public void setOverwritableSettings(List<Setting> overwritableSettings) {
        this.overwritableSettings = overwritableSettings;
    }
    
    public void addOverwritableSettings(List<Setting> settings) {
        this.overwritableSettings.addAll(settings);
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

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
