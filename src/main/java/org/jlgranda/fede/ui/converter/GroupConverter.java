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
package org.jlgranda.fede.ui.converter;


//import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.fede.ejb.GroupService;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.NoResultException;
import org.jpapi.model.profile.Subject;

/**
 *
 * @author jlgranda
 */
@RequestScoped
@FacesConverter("org.jlgranda.fede.ui.converter.GroupConverter")
public class GroupConverter implements Converter, Serializable {

    private static final long serialVersionUID = -3057944404700510467L;
    
    @EJB
    private GroupService service;

    @PostConstruct
    public void setup() {
    }

    @PreDestroy
    public void shutdown() {
    }

    @Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {


        if (value != null && !value.isEmpty() && service != null) {
            try {
                return service.find(getKey(value));
            } catch (NoResultException e) {
                return new Subject();
            }

        }

        return null;
    }

    private Long getKey(String value) {
        return Long.valueOf(value.trim());
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object value) {
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }        
    }
}
