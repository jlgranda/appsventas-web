/*
 * Copyright (C) 2016 jlgranda
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
package org.jlgranda.fede.ui.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.Organization;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.model.talentohumano.JobRole;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.TaxType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.omnifaces.el.functions.Strings;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Utilidades para la construcción de vistas
 *
 * @author jlgranda
 */
//@ManagedBean(name = "ui")
@Named(value = "ui")// Or @ManagedBean
@ApplicationScoped
public class UI {

    @Inject
    private SettingHome settingHome;

    @PostConstruct
    public void init() {
    }

    public Organization.Type[] getOrganizationTypes() {
        return Organization.Type.values();
    }

    public DocumentType[] getDocumentTypes() {
        return DocumentType.values();
    }

    public ProductType[] getProductTypes() {
        return ProductType.values();
    }

    public Group.Type[] getGroupTypes() {
        return Group.Type.values();
    }

    public EmissionType[] getEmissionTypes() {
        return EmissionType.values();
    }

    public FacturaElectronica.DocumentType[] getDocumentFacturaTypes() {
        return FacturaElectronica.DocumentType.values();
    }

    public TaxType[] getTaxTypes() {
        return TaxType.values();
    }

    public List<SelectItem> getDocumentTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (DocumentType t : getDocumentTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public List<SelectItem> getProductTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (ProductType t : getProductTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public SelectItem[] getProductTypesAsSelectItem(List<Product> entities) {
        boolean selectOne = true;
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Product x : entities) {
            items[i++] = new SelectItem(x, x.getName());
        }
        return items;
    }

    public SelectItem[] getProductKardexAsSelectItem(List<Product> entities) {
        boolean selectOne = true;
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Product x : entities) {
            items[i++] = new SelectItem(x, x.getName());
        }
        return items;
    }

    public SelectItem[] getInvoiceAsSelectItem(List<Invoice> entities) {
        boolean selectOne = true;
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Invoice x : entities) {
            items[i++] = new SelectItem(x, x.getName());
        }
        return items;
    }

    public List<SelectItem> getGroupTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (Group.Type t : getGroupTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public SelectItem[] getGroupTypesAsSelectItem(List<Group> entities) {
        boolean selectOne = true;
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Group x : entities) {
            items[i++] = new SelectItem(x, x.getName());
        }
        return items;
    }

    public SelectItem[] getAccountAsSelectItem(List<Account> entities) {
        boolean selectOne = true;
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Account x : entities) {
            items[i++] = new SelectItem(x, x.getCode() + " - " + x.getName());
        }
        return items;
    }

    public List<SelectItem> getAccountTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem("DEBE", "DEBE");
        items.add(item);
        item = new SelectItem("HABER", "HABER");
        items.add(item);
        return items;
    }

    public List<SelectItem> getRecordTemplatesTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);

        return items;
    }

    public List<SelectItem> getOperationTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);
        item = new SelectItem(KardexDetail.OperationType.EXISTENCIA_INICIAL, "INVENTARIO INICIAL");
        items.add(item);
        item = new SelectItem(KardexDetail.OperationType.PRODUCCION, "PRODUCCIÓN");
        items.add(item);
        item = new SelectItem(KardexDetail.OperationType.SALIDA_INVENTARIO, "SALIDA DE INVENTARIO");
        items.add(item);
//        item = new SelectItem(KardexDetail.OperationType.COMPRA.toString(), KardexDetail.OperationType.COMPRA.toString());
//        items.add(item);
//        item = new SelectItem(KardexDetail.OperationType.VENTA.toString(), KardexDetail.OperationType.VENTA.toString());
//        items.add(item);
//        item = new SelectItem(KardexDetail.OperationType.DEVOLUCION_COMPRA.toString(), KardexDetail.OperationType.DEVOLUCION_COMPRA.toString());
//        items.add(item);
//        item = new SelectItem(KardexDetail.OperationType.DEVOLUCION_VENTA.toString(), KardexDetail.OperationType.DEVOLUCION_VENTA.toString());
//        items.add(item);

        return items;
    }

    public List<SelectItem> getMeasuresTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
//        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
//        items.add(item);
        item = new SelectItem("UNIDAD", "[Unit] Unidad");
        items.add(item);
        item = new SelectItem("KILOGRAMO", "[kg] Kilogramo");
        items.add(item);
        item = new SelectItem("GRAMO", "[g] Gramo");
        items.add(item);
        item = new SelectItem("MILIGRAMO", "[mg] Miligramo");
        items.add(item);
        item = new SelectItem("LITRO", "[l] Litro");
        items.add(item);
        item = new SelectItem("MILILITRO", "[ml] Mililitro");
        items.add(item);

        return items;
    }

    public List<SelectItem> getTaxTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (TaxType t : getTaxTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public List<SelectItem> getEmisionTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (EmissionType t : getEmissionTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public List<SelectItem> getEmisionCompraTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (EmissionType t : getEmissionTypes()) {
            if (t.equals(EmissionType.PURCHASE_CASH) || t.equals(EmissionType.PURCHASE_CREDIT)) {
                item = new SelectItem(t, I18nUtil.getMessages(t.name()));
                items.add(item);
            }
        }
        return items;
    }

    public List<SelectItem> getDocumentFacturaTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (FacturaElectronica.DocumentType t : getDocumentFacturaTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public List<SelectItem> getOrganizationTypesAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        for (Organization.Type t : getOrganizationTypes()) {
            item = new SelectItem(t, I18nUtil.getMessages(t.name()));
            items.add(item);
        }
        return items;
    }

    public static SelectItem[] getSelectItems(List<?> entities, boolean selectOne) {
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Object x : entities) {
            items[i++] = new SelectItem(x, x.toString());
        }
        return items;
    }

    public static SelectItem[] getSettingAsSelectItems(List<Setting> entities, boolean selectOne) {
        int size = selectOne ? entities.size() + 1 : entities.size();
        SelectItem[] items = new SelectItem[size];
        int i = 0;
        if (selectOne) {
            items[0] = new SelectItem("", I18nUtil.getMessages("common.choice"));
            i++;
        }
        for (Setting x : entities) {
            items[i++] = new SelectItem(x.getName(), x.getLabel());
        }
        return items;
    }

    public List<SelectItem> getJobRoleAsSelectItems(List<JobRole> values) {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);
        for (JobRole o : values) {
            item = new SelectItem(o.getId(), o.getName());
            items.add(item);
        }

        return items;
    }

    public List<SelectItem> getPaymentMethodsAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem("EFECTIVO", "EFECTIVO");
        items.add(item);
        item = new SelectItem("TARJETA CREDITO", "TARJETA CREDITO");
        items.add(item);
        item = new SelectItem("TARJETA DEBITO", "TARJETA DEBITO");
        items.add(item);

        return items;
    }

    public List<SelectItem> getValuesAsSelectItem(List<Object> values) {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);
        for (Object o : values) {
            item = new SelectItem(cleanValue(o), cleanValue(o).toString());
            items.add(item);
        }

        return items;
    }

    public List<SelectItem> getOrganizationsAsSelectItem(List<Organization> organizations) {

        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);
        for (Organization o : organizations) {
            item = new SelectItem(cleanValue(o), o.getInitials());
            items.add(item);
        }

        return items;
    }

    public List<SelectItem> getAccountsAsSelectItem(List<Account> accounts) {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        items.add(item);
        System.out.println(":::accounts::::"+accounts);
        for (Account o : accounts) {
            item = new SelectItem(cleanValue(o), o.getName());
            items.add(item);
        }

        return items;
    }
    
    public List<SelectItem> getReportTypeAsSelectItem() {
        List<SelectItem> items = new ArrayList<>();
        SelectItem item = null;
//        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
//        items.add(item);
        item = new SelectItem("LISTA", "LISTA");
        items.add(item);
        item = new SelectItem("INDIVIDUAL", "INDIVIDUAL");
        items.add(item);

        return items;
    }

    /**
     * Calcula el tamaño de contenedor para el tamaño de elementos identificado
     * por size
     *
     * @param size
     * @return el contenedor adecuado para size
     */
    public int calculeContainer(long size) {
        return (int) (100 / size);
    }

    private Object cleanValue(Object value) {

        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            return value;
        }

        String cleaned = value.toString();

        if (cleaned.contains("*")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned;
    }

    /**
     * Imprime emoticons para ocultar la cantidad factor
     *
     * @param total
     * @param gap
     * @return
     */
    @Deprecated
    public String calculeEmoticon(BigDecimal total, int gap) {
        String emoticon = "<i class=\"fa  fa-flag-o\"></i>";
        int half_gap = gap / 2;
        if (total.compareTo(BigDecimal.valueOf(gap)) > 0) {
            int factor;
            factor = total.intValue() / gap;
            emoticon = "";
            for (int i = 0; i < factor; i++) {
                emoticon = emoticon + "<i class=\"fa fa-flag\"></i>";
            }

            BigDecimal excedente = total.subtract(new BigDecimal(factor * gap));
            if (excedente.compareTo(BigDecimal.valueOf(half_gap)) > 0) {
                emoticon = emoticon + "<i class=\"fa fa-flag-checkered\"></i>";
            } else {
                emoticon = emoticon + "<i class=\"fa fa-flag-o\"></i>";
            }
        } else if (total.compareTo(BigDecimal.valueOf(half_gap)) > 0) {
            emoticon = "<i class=\"fa fa-flag-checkered\"></i>";
        }
        return emoticon;
    }

    /**
     * Verifica que se haya cumplido con el salto dado para notificación de
     * cumplimiento de totales
     *
     * @param total
     * @param gap
     * @return true si se ha completa la mitad y/o el total, falso en caso
     * contrario
     */
    public static boolean isOver(BigDecimal total, int gap) {
        boolean isOver = false;
        int half_gap = gap / 2;
        int factor;
        if (total.compareTo(BigDecimal.valueOf(gap)) > 0) {
            factor = total.intValue() % gap;
            isOver = factor == 0;
        } else if (total.compareTo(BigDecimal.valueOf(half_gap)) > 0) {
            factor = total.intValue() % half_gap;
            isOver = factor == 0;
        }
        return isOver;
    }

    public static Integer calculePorcentaje(int pageWidth, int porcentaje) {

        double factor = (porcentaje / (double) 100);
        int valor = (int) (pageWidth * factor);
        return valor;
    }

    public String renderer(String template, Subject entity) {
        if (entity == null) {
            return "";
        }
        //TODO Aplicar template via velocity
        return entity.getFullName() + ", " + entity.getCode() + ", " + entity.getDescription();
    }

    /**
     * https://stackoverflow.com/questions/8207325/display-dynamic-image-from-database-or-remote-source-with-pgraphicimage-and-str
     *
     * @param image
     * @return
     * @throws IOException
     */
    public static StreamedContent translateImageFromDB(byte[] image) throws IOException {
        if (image != null) {
            String contentType = "image/png";
            String name = UUID.randomUUID().toString();
            ByteArrayInputStream is = new ByteArrayInputStream(image);
            return DefaultStreamedContent.builder().contentType(contentType).name(name).stream(() -> is).build();
        } else {
            return new DefaultStreamedContent();
        }
    }

    public TimeZone getTimeZone() {
        TimeZone timeZone = TimeZone.getDefault();
        return timeZone;
    }

    public String truncateString(String string) {
        return Strings.abbreviate(string, Integer.valueOf(settingHome.getValue("app.gift.summary.length", "28")));
    }

    public String truncateFilename(String string) {
        return Strings.abbreviate(string, Integer.valueOf(settingHome.getValue("app.documents.filename.length", "14")));
    }

    public static final Object getPropertyValueViaReflection(Object o, String field)
            throws ReflectiveOperationException, IllegalArgumentException, IntrospectionException {
        return new PropertyDescriptor(field, o.getClass()).getReadMethod().invoke(o);
    }

}
