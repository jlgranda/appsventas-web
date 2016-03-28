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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador de entidades GGroup
 * @author jlgranda
 */
@Named
@RequestScoped
public class GroupHome extends FedeController implements Serializable {
    
    private static final long serialVersionUID = -1007161141552849702L;
    
    Logger logger = LoggerFactory.getLogger(GroupHome.class);
    
    @EJB
    GroupService groupService;
    

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    /**
     * Crea los grupos por defecto a partir de una cadena 
     * con el formato name {color, icon}, para la instancia <tt>Subject</tt>
     * 
     * @param subject 
     */
    public void createDefaultGroups(Subject subject) {
        
        Map<String, String> props = new HashMap<String, String>();
        
        //email settings
        props.put("salud", "Salud {1, panel-primary, fa fa-heartbeat fa-5x, fede}");
        props.put("alimentos", "Alimentos {2, panel-sucess, fa fa-icon-shopping-cart fa-5x, fede}");
        props.put("ropa", "Ropa {3, panel-green, fa fa-tag fa-5x, fede}");
        props.put("educacion", "Educación {4, panel-yellow, fa fa-graduation-cap fa-5x, fede}");
        props.put("vivienda", "Vivienda {5, panel-red, fa fa-home fa-5x, fede}");
        props.put("favorito", "Favoritos {1, panel-red, fa fa-heart-o, " + SettingNames.GENERAL_MODULE +"}");

        Group group = null;
        String value = null;
        Short orden = null;
        String icon = null;
        String module = null;
        String color = null;
        String attrs = null;
        for (String key : props.keySet()){
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
            
            logger.info("Added group id: {}, code: {}, name: [{}], props: name: [{}, {}]", group.getId(), group.getCode(), group.getName(), group.getColor(), group.getIcon());
        }
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
