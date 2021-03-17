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
package org.jlgranda.fede.controller.inventory;

import org.jlgranda.fede.controller.*;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.ui.model.LazyGroupDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador de entidades GGroup
 *
 * @author jlgranda
 */
@Named
@RequestScoped
public class GroupInventoryHome extends FedeController implements Serializable {
    
    private static final long serialVersionUID = -1007161141552849702L;
    
    Logger logger = LoggerFactory.getLogger(GroupInventoryHome.class);
    
    @Inject
    private SettingHome settingHome;
    
    @Inject
    private Subject subject;
    
    @EJB
    private GroupService groupService;
    
    private LazyGroupDataModel lazyDataModel;
    
    private Group.Type groupType;
    
    private Group group;
    
    private Long groupId;
    
    @Inject
    private OrganizationData organizationData;
    
    @PostConstruct
    private void init() {
        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.GROUP_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setGroupType(Group.Type.PRODUCT);
        if (Group.Type.PRODUCT.equals(getGroupType())) {
            Group instance = groupService.createInstance(getGroupType());//Instancia de Grupo
            instance.setColor("panel-primary");
            instance.setIcon("fa fa-object-group");
            instance.setModule("inventory");
            setGroup(instance);//Instancia de Grupo
        }
        setOutcome("inventory-groups");
        filter();
        
    }

    /**
     * Crea los grupos por defecto a partir de una cadena con el formato name
     * {color, icon}, para la instancia <tt>Subject</tt>
     *
     * @param subject
     */
    public void createDefaultGroups(Subject subject) {
        
        Map<String, String> props = new HashMap<>();

        //email settings
        props.put("salud", "Salud {1, panel-primary, fa fa-heartbeat fa-5x, fede}");
        props.put("alimentos", "Alimentos {2, panel-sucess, fa fa-icon-shopping-cart fa-5x, fede}");
        props.put("ropa", "Ropa {3, panel-green, fa fa-tag fa-5x, fede}");
        props.put("educacion", "Educación {4, panel-yellow, fa fa-graduation-cap fa-5x, fede}");
        props.put("vivienda", "Vivienda {5, panel-red, fa fa-home fa-5x, fede}");
        props.put("favorito", "Favoritos {1, panel-red, fa fa-heart-o, " + SettingNames.GENERAL_MODULE + "}");
        
        Group group = null;
        String value = null;
        Short orden = null;
        String icon = null;
        String module = null;
        String color = null;
        String attrs = null;
        for (String key : props.keySet()) {
            value = props.get(key);
            attrs = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            orden = Short.valueOf(attrs.split(",")[0]);
            color = attrs.split(",")[1];
            icon = attrs.split(",")[2];
            module = attrs.split(",")[3];
            value = value.substring(0, (value.indexOf("{") - 1));
            group = groupService.createInstance();
            group.setCode(key.trim());
            group.setName(value.trim());
            group.setOwner(subject);
            group.setColor(color.trim());
            group.setIcon(icon.trim());
            group.setModule(module.trim()); //Marcar con null para el caso de etiquetas generales
            group.setOrden(orden);
            group.setGroupType(Group.Type.LABEL);
            
            groupService.save(group);

            //logger.info("Added group id: {}, code: {}, name: [{}], props: name: [{}, {}]", group.getId(), group.getCode(), group.getName(), group.getColor(), group.getIcon());
        }
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }
    
    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), getGroupType()));
        }
        return this.groups;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public Group getGroup() {
        if (this.groupId != null && !this.group.isPersistent()) {
            this.group = groupService.find(groupId);
        }
        return group;
    }
    
    public void setGroup(Group group) {
        this.group = group;
    }
    
    public Group.Type getGroupType() {
        return groupType;
    }
    
    public void setGroupType(Group.Type groupType) {
        this.groupType = groupType;
    }
    
    public LazyGroupDataModel getLazyDataModel() {
        return lazyDataModel;
    }
    
    public void setLazyDataModel(LazyGroupDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }
    
    public List<Group> getGroupsTypeProduct() {
//        return groupService.findByType(Group.Type.PRODUCT);
        return groupService.findByOrganizationAndType(this.organizationData.getOrganization(), Group.Type.PRODUCT);
    }
    
    public void clear() {
        filter();
    }
    
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyGroupDataModel(groupService);
        }
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
//        lazyDataModel.setOwner(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());
        lazyDataModel.setGroupType(getGroupType());
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
    
    public void save() {
        if (group.isPersistent()) {
            group.setLastUpdate(Dates.now());
        } else {
            group.setAuthor(this.subject);
            group.setOwner(this.subject);
            group.setOrganization(this.organizationData.getOrganization());
        }
        groupService.save(group.getId(), group);
    }
    
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccioar a RIDE de Objeto Seleccionado
            if (event != null && event.getObject() != null) {
                Group g = (Group) event.getObject();
                redirectTo("/pages/fede/inventory/group.jsf?groupId=" + g.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
        
    }
    
    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
