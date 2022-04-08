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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import org.jlgranda.fede.model.reportes.Reporte;
import org.omnifaces.converter.SelectItemsConverter;

/**
 *
 * @author usuario
 */
@FacesConverter("org.jlgranda.fede.ui.converter.ReporteConverter")
public class ReporteConverter extends SelectItemsConverter {

    /**
     * Gets the as string.
     *
     * @param context the context
     * @param component the component
     * @param value the value
     * @return the as string
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        Long id = value instanceof Reporte ? ((Reporte) value).getId() : null;
        return id != null ? String.valueOf(id) : null;
    }

}
