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

import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.NoResultException;
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.profile.Subject;

/**
 *
 * @author jlgranda
 */
@RequestScoped
@FacesConverter("org.jlgranda.fede.ui.converter.ProductComprasConverter")
public class ProductComprasConverter implements Converter, Serializable {

    private static final long serialVersionUID = -3361609066820809465L;

    @EJB
    private ProductCache service;

    @EJB
    private ProductService productService;
    @PostConstruct
    public void setup() {
    }

    @PreDestroy
    public void shutdown() {
    }

    @Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
        
        Product productNew = null;

        if (value != null && !value.isEmpty() && service != null) {
            try {
                productNew =  service.lookup(getKey(value));
                if(productNew==null){
                    productNew = productService.createInstance();
                    productNew.setName(value);
                }
                return productNew;
            } catch (NoResultException e) {
                return new Subject();
            }

        }

        return null;
    }

    private Long getKey(String value) {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(value);
        String key = null;
        while (m.find()) {
            key = m.group();
        }
        if (key == null) {
            return -1L;
        } else {
            return Long.valueOf(key);
        }
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
