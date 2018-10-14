/*
 * Copyright (C) 2018 jlgranda
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
package org.jlgranda.fede.faces.validator;

import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import org.jpapi.model.BussinesEntity;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Interpolator;
import org.jpapi.util.Strings;

/**
 * Validador de número de comanda
 * @author jlgranda
 */
@FacesValidator("comandaValidator")
@RequestScoped
public class ComandaValidator  implements Validator  {
    
    @EJB
    private InvoiceService service;
    
    private String message;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String key = String.valueOf(value);
        if (Strings.isNullOrEmpty(key)){
            message = Interpolator.interpolate(
                        I18nUtil.getMessages("validation.requiredIdentificationNumber"),
                        new Object[0]);
                throw new ValidatorException(new FacesMessage(message));
        } else {
            List<BussinesEntity> result = service.findByNamedQueryWithLimit("BussinesEntity.findByCode", 1, key);
            if (!result.isEmpty()){
                message = Interpolator.interpolate(
                        I18nUtil.getMessages("validation.comandaUniqueValue"),
                        new Object[0]);
                throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de validación", message));
            } else {

            }
        }
    }
    
}
