/*
 * Copyright (C) 2016 jlgranda
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

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.PersistentObject;
import org.omnifaces.converter.SelectItemsConverter;
import org.picketlink.idm.jpa.model.sample.simple.GroupTypeEntity;

/**
 *
 * @author jlgranda
 */
@RequestScoped
public class BussinesEntityConverter extends SelectItemsConverter {

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        String key = null;
        if (value instanceof GroupTypeEntity) {
            key = ((GroupTypeEntity) value).getName();
        } else {
            Long id = null;
            if (value instanceof BussinesEntity) {
                id = ((BussinesEntity) value).getId();
            } else if (value instanceof PersistentObject) {
                id = ((PersistentObject) value).getId();
            }
            key = (id != null) ? String.valueOf(id) : null;
        }
        return key;
    }
}