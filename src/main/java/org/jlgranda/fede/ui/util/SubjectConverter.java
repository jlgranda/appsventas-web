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
import java.util.List;
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

    private static List<Subject> subjects;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value.trim().equals("")) {
            return null;
        } else {
            if (SubjectConverter.subjects != null) {
                for (Subject subject : SubjectConverter.subjects) {
                    if (subject.getId() == Long.parseLong(value)) {
                        return subject;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (((Subject) value) != null) {
            return ((Subject) value).getId().toString();
        }
        return "";
    }

    public static List<Subject> getSubjects() {
        return subjects;
    }

    public static void setSubjects(List<Subject> subjects) {
        SubjectConverter.subjects = subjects;
    }

}
