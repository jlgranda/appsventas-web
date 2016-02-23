/*
 * Copyright (C) 2016 Jorge
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
package org.jlgranda.fede.ui.util;

import com.jlgranda.fede.ejb.SubjectService;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.jpapi.model.profile.Subject;

/**
 *
 * @author Jorge
 */
@FacesConverter("subjectConverter")
public class SubjectConverter implements Converter {

    @EJB
    private SubjectService subjectService;

    @Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String string) {
        return subjectService.find(Long.parseLong(string));
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object o) {
        Subject subject = ((Subject) o);
        if (subject == null) {
            return null;
        }
        return subject.getId() + "";
    }

}
