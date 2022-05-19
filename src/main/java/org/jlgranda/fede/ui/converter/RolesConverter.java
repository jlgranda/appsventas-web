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
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import org.omnifaces.converter.SelectItemsConverter;
import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author usuario
 */
@RequestScoped
@FacesConverter("org.jlgranda.fede.ui.converter.RolesConverter")
public class RolesConverter extends SelectItemsConverter {

//    private static final Logger LOG = LoggerFactory.getLogger(RolesConverter.class);
//
//    public Object getAsObject(FacesContext context, UIComponent component, String value) {
//        LOG.trace("String value: {}", value);
//        return getObjectFromUIPickListComponent(component, value);
//    }
//
//    public String getAsString(FacesContext context, UIComponent component, Object object) {
//        String string;
//        LOG.trace("Object value: {}", object);
//        if (object == null) {
//            string = "";
//        } else {
//            try {
//                string = String.valueOf(((Roles) object).getName());
//            } catch (ClassCastException cce) {
//                throw new ConverterException();
//            }
//        }
//        return string;
//    }
//
//    @SuppressWarnings("unchecked")
//    private Roles getObjectFromUIPickListComponent(UIComponent component, String value) {
//        final DualListModel<Roles> dualList;
//        try {
//            dualList = (DualListModel<Roles>) ((PickList) component).getValue();
//            Roles team = getObjectFromList(dualList.getSource(), Integer.valueOf(value));
//            if (team == null) {
//                team = getObjectFromList(dualList.getTarget(), Integer.valueOf(value));
//            }
//
//            return team;
//        } catch (ClassCastException cce) {
//            throw new ConverterException();
//        } catch (NumberFormatException nfe) {
//            throw new ConverterException();
//        }
//    }
//
//    private Roles getObjectFromList(final List<?> list, final Integer identifier) {
//        for (final Object object : list) {
//            final Roles team = (Roles) object;
//            if (team.getName().equals(identifier)) {
//                return team;
//            }
//        }
//        return null;
//    }
    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        PickList p = (PickList) component;
        DualListModel dl = (DualListModel) p.getValue();
        if (dl.getSource() != null && dl.getSource().size() > 0) {
            for (int i = 0; i < dl.getSource().size(); i++) {
                if (((Roles) dl.getSource().get(i)).getName().contentEquals(submittedValue)) {
                    return dl.getSource().get(i);
                }
            }
        }
        if (dl.getTarget() != null && dl.getTarget().size() > 0) {
            for (int i = 0; i < dl.getTarget().size(); i++) {
//                System.out.println("SubmittedValue: " + submittedValue);
//                System.out.println("dl.getTarget().get(i)).getName(): " + ((Roles) dl.getTarget().get(i)).getName());
//                System.out.println("if: " + ((Roles) dl.getTarget().get(i)).getName().equals(submittedValue));
                if (((Roles) dl.getTarget().get(i)).getName().equals(submittedValue)) {
                    return dl.getTarget().get(i);
                }
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
