/*
 * Copyright (C) 2022 usuario
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
package org.jlgranda.fede.ui.converter;

import com.jlgranda.shiro.Roles;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import org.omnifaces.converter.SelectItemsConverter;
import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;

/**
 *
 * @author usuario
 */
@RequestScoped
@FacesConverter("org.jlgranda.fede.ui.converter.RolesConverter")
public class RolesConverter extends SelectItemsConverter {
    
    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        PickList p = (PickList) component;
        DualListModel dl = (DualListModel) p.getValue();
        for (int i = 0; i < dl.getSource().size(); i++) {
            if ( ( (Roles) dl.getSource().get(i)).getName().contentEquals(submittedValue)) {
                return dl.getSource().get(i);
            }
        }
        for (int i = 0; i < dl.getTarget().size(); i++) {
            if ( ( (Roles) dl.getSource().get(i)).getName().contentEquals(submittedValue)) {
                return dl.getTarget().get(i);
            }
        }
        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        String key = null;
        if (value instanceof Roles) {
            key = ((Roles) value).getName();
        } 
        return key;
    }
}