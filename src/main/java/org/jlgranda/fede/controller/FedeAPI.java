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

import java.io.Serializable;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import org.jlgranda.fede.util.FacturaUtil;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.sri.jaxb.factura.v110.Factura;
import org.jpapi.model.TaxRateIVAType;
import org.jpapi.model.TaxType;
import org.jpapi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@SessionScoped
@Named
public class FedeAPI implements Serializable {
    private static final long serialVersionUID = 43323578487360899L;
    
    Logger  logger = LoggerFactory.getLogger(FedeAPI.class);
    
    public Factura readFactura(FacturaElectronica facturaElectronica){
        return readFactura(facturaElectronica.getContenido());
    }
    
    public Factura readFactura(String xml){
        if (Strings.isNullOrEmpty(xml))
            return null;
        return FacturaUtil.read(xml);
    }
    
    public String translate(String key, Enum e){
        
        if (e instanceof TaxRateIVAType){
                TaxRateIVAType t = (TaxRateIVAType) e;
                return t.translate(TaxRateIVAType.encode(key));
        } else if (e instanceof TaxType){
                TaxType t = (TaxType) e;
                return t.translate(TaxType.encode(key));
        }
        return "undefined!";
    }
    
    public String translateTaxRateIVA(String key){
        return translate(key, TaxRateIVAType.NONE);
    }
    
    public String summary(FacturaElectronica facturaElectronica){
        Factura f = readFactura(facturaElectronica);
        if (f == null) return facturaElectronica.getDescription();
        StringBuilder buffer = new StringBuilder();
        int index = 0;
        String str;
        char delimitador = '\0';
        char separador =  ',';
//        for (Factura.Detalles.Detalle d : f.getDetalles().getDetalle()){
//            str = d.getDescripcion();
//            str = str.replaceAll("\'", "\\\'");
//            buffer.append(delimitador)
//            .append(str)
//            .append(delimitador);
//           
//            if (index < (f.getDetalles().getDetalle().size() - 1)) {
//                buffer.append(separador);
//                buffer.append(' ');
//            }
//            index++;
//        }
        
        return buffer.toString();
    }
    
    public static void main(String[] args) {
        FedeAPI api = new FedeAPI();
        System.err.println("---> " +  api.translate("0", TaxRateIVAType.NONE));
        System.err.println("---> " +  api.translate("2", TaxRateIVAType.NONE));
        System.err.println("---> " +  api.translate("6", TaxRateIVAType.NONE));
        System.err.println("---> " +  api.translate("7", TaxRateIVAType.NONE));
    }
}
